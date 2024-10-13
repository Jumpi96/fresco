package com.fresco

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, GetItemRequest, ScanRequest, ScanResult}
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

class DynamoDBService(awsConfig: Config)(implicit ec: ExecutionContext) {
  val INGREDIENTS_TABLE = awsConfig.getConfig("storage").getString("ingredientsTableName")

  val dynamoDBClient = AmazonDynamoDBClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(
      new BasicAWSCredentials(awsConfig.getString("accessKey"), awsConfig.getString("secretKey"))))
    .build();

  def getIngredients(lastEvaluatedId: Option[String] = None, limit: Int = 50): Future[(Seq[Ingredient], Option[String])] = {
    val scanRequest = new ScanRequest()
      .withTableName(INGREDIENTS_TABLE)
      .withLimit(limit)

    if (lastEvaluatedId.isDefined) {
      scanRequest.withExclusiveStartKey(Map("id" -> new AttributeValue().withS(lastEvaluatedId.get)).asJava)
    }

    Future {
      val result: ScanResult = dynamoDBClient.scan(scanRequest)

      // Convert the result into a sequence of Ingredient case class instances
      val ingredients: Seq[Ingredient] = result.getItems.asScala.map { item =>
        Ingredient(
          id = item.get("id").getS,
          name = item.get("name").getS,
          imagePath = Option(item.get("imagePath")).map(_.getS)
        )
      }.toSeq

      // Extract the last evaluated key and return it as a String
      val lastEvaluatedId: Option[String] = Option(result.getLastEvaluatedKey).flatMap { keyMap =>
        Option(keyMap.get("id")).map(_.getS)
      }

      // Return ingredients and the lastEvaluatedId (for pagination)
      (ingredients, lastEvaluatedId)
    }.recover {
      case ex: Exception =>
        throw new RuntimeException(s"Error fetching ingredients: ${ex.getMessage}")
    }
  }

  def getIngredient(id: String): Future[Option[Ingredient]] = {
    Future {
      val getItemRequest = new GetItemRequest()
        .withTableName(INGREDIENTS_TABLE)
        .withKey(Map("id" -> new AttributeValue().withS(id)).asJava)

      val result = dynamoDBClient.getItem(getItemRequest)
      Option(result.getItem).map { item =>
        Ingredient(
          id = item.get("id").getS,
          name = item.get("name").getS,
          imagePath = Option(item.get("imagePath")).map(_.getS)
        )
      }
    }.recover {
      case ex: Exception =>
        throw new RuntimeException(s"Error fetching ingredient $id: ${ex.getMessage}")
    }
  }
}
