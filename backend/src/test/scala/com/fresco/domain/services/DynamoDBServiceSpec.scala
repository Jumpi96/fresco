package com.fresco.domain.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, GetItemRequest, GetItemResult}
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
        val service = new DynamoDBService(mockDynamoDBClient, "ingredients")

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
        val service = new DynamoDBService(mockDynamoDBClient, "ingredients")

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
        val service = new DynamoDBService(mockDynamoDBClient, "ingredients")

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
        val service = new DynamoDBService(mockDynamoDBClient, "ingredients")

        val id = "999"

        when(mockDynamoDBClient.getItem(any[GetItemRequest])).thenThrow(new RuntimeException("DynamoDB error"))

        val result = service.getIngredient(id)

        whenReady(result.failed) { exception =>
          exception shouldBe a[RuntimeException]
          exception.getMessage should include(s"Error fetching ingredient $id")
        }
      }
    }
  }
}
