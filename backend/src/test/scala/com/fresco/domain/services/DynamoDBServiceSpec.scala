package com.fresco.domain.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, GetItemRequest, GetItemResult, ScanRequest, ScanResult}
import com.fresco.domain.models.Ingredient
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.*

class DynamoDBServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {

  "DynamoDBService" should {
    "getIngredient" should {
      "return an ingredient when it exists" in {
        val mockDynamoDBClient = mock[AmazonDynamoDB]
        val service = new DynamoDBService(mockDynamoDBClient, "ingredients", "recipes", "favourites")

        val id = "123"
        val name = "Salt"
        val imagePath = "path/to/image.jpg"

        val item = Map(
          "id" -> new AttributeValue().withS(id),
          "name" -> new AttributeValue().withS(name),
          "imagePath" -> new AttributeValue().withS(imagePath)
        ).asJava

        val getItemResult = new GetItemResult().withItem(item)

        when(mockDynamoDBClient.getItem(any[GetItemRequest])).thenReturn(getItemResult)

        val result = service.getIngredient(id)

        whenReady(result) { ingredient =>
          ingredient shouldBe defined
          ingredient.get shouldBe Ingredient(id, name, Some(imagePath))
        }
      }

      "return None when the ingredient doesn't exist" in {
        val mockDynamoDBClient = mock[AmazonDynamoDB]
        val service = new DynamoDBService(mockDynamoDBClient, "ingredients", "recipes", "favourites")

        val id = "456"

        val getItemResult = new GetItemResult()

        when(mockDynamoDBClient.getItem(any[GetItemRequest])).thenReturn(getItemResult)

        val result = service.getIngredient(id)

        whenReady(result) { ingredient =>
          ingredient shouldBe None
        }
      }

      "handle missing imagePath" in {
        val mockDynamoDBClient = mock[AmazonDynamoDB]
        val service = new DynamoDBService(mockDynamoDBClient, "ingredients", "recipes", "favourites")

        val id = "789"
        val name = "Pepper"

        val item = Map(
          "id" -> new AttributeValue().withS(id),
          "name" -> new AttributeValue().withS(name)
        ).asJava

        val getItemResult = new GetItemResult().withItem(item)

        when(mockDynamoDBClient.getItem(any[GetItemRequest])).thenReturn(getItemResult)

        val result = service.getIngredient(id)

        whenReady(result) { ingredient =>
          ingredient shouldBe defined
          ingredient.get shouldBe Ingredient(id, name, None)
        }
      }

      "throw RuntimeException when DynamoDB client throws an exception" in {
        val mockDynamoDBClient = mock[AmazonDynamoDB]
        val service = new DynamoDBService(mockDynamoDBClient, "ingredients", "recipes", "favourites")

        val id = "999"

        when(mockDynamoDBClient.getItem(any[GetItemRequest])).thenThrow(new RuntimeException("DynamoDB error"))

        val result = service.getIngredient(id)

        whenReady(result.failed) { exception =>
          exception shouldBe a[RuntimeException]
          exception.getMessage should include(s"Error fetching ingredient $id")
        }
      }
    }
    "getRecipes" should {
      "return recipes and lastEvaluatedId when successful" in {
        val mockDynamoDBClient = mock[AmazonDynamoDB]
        val service = new DynamoDBService(mockDynamoDBClient, "ingredients", "recipes", "favourites")

        val scanResult = new ScanResult()
          .withItems(
            Map(
              "id" -> new AttributeValue().withS("1"),
              "name" -> new AttributeValue().withS("Recipe 1"),
              "totalTime" -> new AttributeValue().withS("30 mins"),
              "websiteUrl" -> new AttributeValue().withS("http://example.com"),
              "imagePath" -> new AttributeValue().withS("path/to/image.jpg"),
              "pdfPath" -> new AttributeValue().withS("path/to/pdf.pdf"),
              "macros" -> new AttributeValue().withM(
                Map(
                  "fats" -> new AttributeValue().withN("10.0"),
                  "carbs" -> new AttributeValue().withN("20.0"),
                  "proteins" -> new AttributeValue().withN("15.0")
                ).asJava
              ),
              "ingredientsPerPerson" -> new AttributeValue().withL(
                new AttributeValue().withM(
                  Map(
                    "id" -> new AttributeValue().withS("ing1"),
                    "amount" -> new AttributeValue().withN("100.0"),
                    "unit" -> new AttributeValue().withS("g")
                  ).asJava
                )
              ),
              "steps" -> new AttributeValue().withL(
                new AttributeValue().withM(
                  Map(
                    "index" -> new AttributeValue().withN("1"),
                    "instructions" -> new AttributeValue().withS("Step 1"),
                    "instructionsHTML" -> new AttributeValue().withS("<p>Step 1</p>")
                  ).asJava
                )
              )
            ).asJava
          )
          .withLastEvaluatedKey(Map("id" -> new AttributeValue().withS("1")).asJava)

        when(mockDynamoDBClient.scan(any[ScanRequest])).thenReturn(scanResult)

        val result = service.getRecipes()

        whenReady(result) { case (recipes, lastEvaluatedId) =>
          recipes should have length 1
          recipes.head.id shouldBe "1"
          recipes.head.name shouldBe "Recipe 1"
          lastEvaluatedId shouldBe Some("1")
        }
      }

      "handle pagination with lastEvaluatedId" in {
        val mockDynamoDBClient = mock[AmazonDynamoDB]
        val service = new DynamoDBService(mockDynamoDBClient, "ingredients", "recipes", "favourites")

        val scanResult = new ScanResult().withItems(Seq.empty[java.util.Map[String, AttributeValue]].asJava)

        when(mockDynamoDBClient.scan(any[ScanRequest])).thenReturn(scanResult)

        val result = service.getRecipes(Some("lastId"), 10)

        whenReady(result) { case (recipes, lastEvaluatedId) =>
          recipes should be(empty)
          lastEvaluatedId shouldBe None
        }
      }

      "throw RuntimeException when DynamoDB client throws an exception" in {
        val mockDynamoDBClient = mock[AmazonDynamoDB]
        val service = new DynamoDBService(mockDynamoDBClient, "ingredients", "recipes", "favourites")

        when(mockDynamoDBClient.scan(any[ScanRequest])).thenThrow(new RuntimeException("DynamoDB error"))

        val result = service.getRecipes()

        whenReady(result.failed) { exception =>
          exception shouldBe a[RuntimeException]
          exception.getMessage should include("Error fetching recipes")
        }
      }
    }

    "getRecipe" should {
      "return a recipe when it exists" in {
        val mockDynamoDBClient = mock[AmazonDynamoDB]
        val service = new DynamoDBService(mockDynamoDBClient, "ingredients", "recipes", "favourites")

        val id = "123"
        val item = Map(
          "id" -> new AttributeValue().withS(id),
          "name" -> new AttributeValue().withS("Test Recipe"),
          "totalTime" -> new AttributeValue().withS("30 mins"),
          "websiteUrl" -> new AttributeValue().withS("http://example.com"),
          "imagePath" -> new AttributeValue().withS("path/to/image.jpg"),
          "pdfPath" -> new AttributeValue().withS("path/to/pdf.pdf"),
          "macros" -> new AttributeValue().withM(
            Map(
              "fats" -> new AttributeValue().withN("10.0"),
              "carbs" -> new AttributeValue().withN("20.0"),
              "proteins" -> new AttributeValue().withN("15.0")
            ).asJava
          ),
          "ingredientsPerPerson" -> new AttributeValue().withL(
            new AttributeValue().withM(
              Map(
                "id" -> new AttributeValue().withS("ing1"),
                "amount" -> new AttributeValue().withN("100.0"),
                "unit" -> new AttributeValue().withS("g")
              ).asJava
            )
          ),
          "steps" -> new AttributeValue().withL(
            new AttributeValue().withM(
              Map(
                "index" -> new AttributeValue().withN("1"),
                "instructions" -> new AttributeValue().withS("Step 1"),
                "instructionsHTML" -> new AttributeValue().withS("<p>Step 1</p>")
              ).asJava
            )
          )
        ).asJava

        val getItemResult = new GetItemResult().withItem(item)

        when(mockDynamoDBClient.getItem(any[GetItemRequest])).thenReturn(getItemResult)

        val result = service.getRecipe(id)

        whenReady(result) { recipe =>
          recipe shouldBe defined
          recipe.get.id shouldBe id
          recipe.get.name shouldBe "Test Recipe"
          recipe.get.macros.fats shouldBe 10.0
          recipe.get.ingredients should have length 1
          recipe.get.steps should have length 1
        }
      }

      "return None when the recipe doesn't exist" in {
        val mockDynamoDBClient = mock[AmazonDynamoDB]
        val service = new DynamoDBService(mockDynamoDBClient, "ingredients", "recipes", "favourites")

        val id = "456"

        val getItemResult = new GetItemResult()

        when(mockDynamoDBClient.getItem(any[GetItemRequest])).thenReturn(getItemResult)

        val result = service.getRecipe(id)

        whenReady(result) { recipe =>
          recipe shouldBe None
        }
      }

      "throw RuntimeException when DynamoDB client throws an exception" in {
        val mockDynamoDBClient = mock[AmazonDynamoDB]
        val service = new DynamoDBService(mockDynamoDBClient, "ingredients", "recipes", "favourites")

        val id = "789"

        when(mockDynamoDBClient.getItem(any[GetItemRequest])).thenThrow(new RuntimeException("DynamoDB error"))

        val result = service.getRecipe(id)

        whenReady(result.failed) { exception =>
          exception shouldBe a[RuntimeException]
          exception.getMessage should include(s"Error fetching recipe $id")
        }
      }
    }
  }
}
