import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import com.fresco.domain.models.Ingredient
import com.fresco.domain.repositories.IngredientRepository
import com.fresco.domain.services.{DynamoDBService, S3Service}
import org.mockito.Mockito.when

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IngredientRepositorySpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  "IngredientRepository" should {
    "return ingredient with presigned URL when image path exists" in {
      val mockDynamoDBService = mock[DynamoDBService]
      val mockS3Service = mock[S3Service]
      val repository = new IngredientRepository(mockDynamoDBService, mockS3Service)

      val id = "123"
      val imagePath = "path/to/image.jpg"
      val presignedUrl = "https://presigned-url.com/image.jpg"
      val ingredient = Ingredient(id, "Test Ingredient", Some(imagePath))

      when(mockDynamoDBService.getIngredient(id)).thenReturn(Future.successful(Some(ingredient)))
      when(mockS3Service.generatePresignedUrl(imagePath)).thenReturn(Future.successful(new java.net.URL(presignedUrl)))

      val result = repository.getIngredient(id)

      whenReady(result) { maybeIngredient =>
        maybeIngredient shouldBe defined
        maybeIngredient.get.imagePath shouldBe Some(presignedUrl)
      }
    }

    "return ingredient without image path when S3 service fails" in {
      val mockDynamoDBService = mock[DynamoDBService]
      val mockS3Service = mock[S3Service]
      val repository = new IngredientRepository(mockDynamoDBService, mockS3Service)

      val id = "456"
      val imagePath = "path/to/image.jpg"
      val ingredient = Ingredient(id, "Test Ingredient", Some(imagePath))

      when(mockDynamoDBService.getIngredient(id)).thenReturn(Future.successful(Some(ingredient)))
      when(mockS3Service.generatePresignedUrl(imagePath)).thenReturn(Future.failed(new RuntimeException("S3 error")))

      val result = repository.getIngredient(id)

      whenReady(result) { maybeIngredient =>
        maybeIngredient shouldBe defined
        maybeIngredient.get.imagePath shouldBe None
      }
    }

    "return ingredient without image path when no image path exists" in {
      val mockDynamoDBService = mock[DynamoDBService]
      val mockS3Service = mock[S3Service]
      val repository = new IngredientRepository(mockDynamoDBService, mockS3Service)

      val id = "789"
      val ingredient = Ingredient(id, "Test Ingredient", None)

      when(mockDynamoDBService.getIngredient(id)).thenReturn(Future.successful(Some(ingredient)))

      val result = repository.getIngredient(id)

      whenReady(result) { maybeIngredient =>
        maybeIngredient shouldBe defined
        maybeIngredient.get.imagePath shouldBe None
      }
    }

    "fail with RuntimeException when ingredient is not found" in {
      val mockDynamoDBService = mock[DynamoDBService]
      val mockS3Service = mock[S3Service]
      val repository = new IngredientRepository(mockDynamoDBService, mockS3Service)

      val id = "999"

      when(mockDynamoDBService.getIngredient(id)).thenReturn(Future.successful(None))

      val result = repository.getIngredient(id)

      whenReady(result.failed) { exception =>
        exception shouldBe a[RuntimeException]
        exception.getMessage shouldBe s"Ingredient with id $id not found"
      }
    }
  }
}
