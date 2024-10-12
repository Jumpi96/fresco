package com.fresco

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.model.ScanRequest
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala

class DynamoDBService(awsConfig: Config)(implicit ec: ExecutionContext) {
  val INGREDIENTS_TABLE = awsConfig.getConfig("storage").getString("ingredientsTableName")

  val dynamoDBClient = AmazonDynamoDBClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(
      new BasicAWSCredentials(awsConfig.getString("accessKey"), awsConfig.getString("secretKey"))))
    .build();

  def getIngredients(): Future[Seq[Ingredient]] = {
    Future {
      val result = dynamoDBClient.scan(
        new ScanRequest()
          .withTableName(INGREDIENTS_TABLE)
          .withLimit(100) // TODO: remove
      )

      // Convert the result into a sequence of Ingredient case class instances
      result.getItems.asScala.map { item =>
        Ingredient(
          id = item.get("id").getS,
          name = item.get("name").getS,
          imagePath = Option(item.get("imagePath")).map(_.getS)
        )
      }.toSeq
    }.recover {
      case ex: Exception =>
        println(s"Error fetching ingredients from DynamoDB: ${ex.getMessage}")
        Seq.empty[Ingredient]
    }
  }
}
