package com.fresco.domain.repositories

import com.fresco.domain.models.{IngredientPerPerson, Macros, Recipe, Step}
import com.fresco.domain.services.{DynamoDBService, S3Service}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RecipeRepositorySpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  val recipeExample: Recipe = Recipe(
    id = "1",
    name = "Recipe1",
    totalTime = "30 minutes",
    websiteUrl = "https://example.com/recipe1",
    imagePath = Some("s3://fresco-storage-bucket/images/554a5243fd2cb9ba4f8b456f.png"),
    cardLink = None,
    macros = Macros(fats = 10.0, carbs = 20.0, proteins = 15.0),
    ingredients = Seq(
      IngredientPerPerson(id = "ing1", amount = 100.0, unit = "g")
    ),
    steps = Seq(
      Step(index = 1, instructions = "Mix ingredients", instructionsHTML = "<p>Mix ingredients</p>")
    )
  )

  "RecipeRepository" should {
    "return recipe with presigned URL when image path exists" in {
      val mockDynamoDBService = mock[DynamoDBService]
      val mockS3Service = mock[S3Service]
      val repository = new RecipeRepository(mockDynamoDBService, mockS3Service)

      val presignedUrl = "https://presigned-url.com/image.jpg"
      val recipe = recipeExample

      when(mockDynamoDBService.getRecipe(recipe.id)).thenReturn(Future.successful(Some(recipe)))
      when(mockS3Service.generatePresignedUrl(recipe.imagePath.get)).thenReturn(Future.successful(new java.net.URL(presignedUrl)))

      val result = repository.getRecipe(recipe.id)

      whenReady(result) { maybeRecipe =>
        maybeRecipe shouldBe defined
        maybeRecipe.get.imagePath shouldBe Some(presignedUrl)
      }
    }

    "return recipe without image path when S3 service fails" in {
      val mockDynamoDBService = mock[DynamoDBService]
      val mockS3Service = mock[S3Service]
      val repository = new RecipeRepository(mockDynamoDBService, mockS3Service)

      val recipe = recipeExample

      when(mockDynamoDBService.getRecipe(recipe.id)).thenReturn(Future.successful(Some(recipe)))
      when(mockS3Service.generatePresignedUrl(recipe.imagePath.get)).thenReturn(Future.failed(new RuntimeException("S3 error")))

      val result = repository.getRecipe(recipe.id)

      whenReady(result) { maybeRecipe =>
        maybeRecipe shouldBe defined
        maybeRecipe.get.imagePath shouldBe None
      }
    }

    "return recipe without image path when no image path exists" in {
      val mockDynamoDBService = mock[DynamoDBService]
      val mockS3Service = mock[S3Service]
      val repository = new RecipeRepository(mockDynamoDBService, mockS3Service)

      val recipe = recipeExample.copy(imagePath = None)

      when(mockDynamoDBService.getRecipe(recipe.id)).thenReturn(Future.successful(Some(recipe)))

      val result = repository.getRecipe(recipe.id)

      whenReady(result) { maybeRecipe =>
        maybeRecipe shouldBe defined
        maybeRecipe.get.imagePath shouldBe None
      }
    }

    "fail with RuntimeException when recipe is not found" in {
      val mockDynamoDBService = mock[DynamoDBService]
      val mockS3Service = mock[S3Service]
      val repository = new RecipeRepository(mockDynamoDBService, mockS3Service)

      val id = "999"

      when(mockDynamoDBService.getRecipe(id)).thenReturn(Future.successful(None))

      val result = repository.getRecipe(id)

      whenReady(result.failed) { exception =>
        exception shouldBe a[RuntimeException]
        exception.getMessage shouldBe s"Recipe with id $id not found"
      }
    }
  }
}
