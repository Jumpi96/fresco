package com.fresco.domain.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, GetItemRequest, ScanRequest, ScanResult}
import com.fresco.domain.models.{Ingredient, IngredientPerPerson, Macros, Recipe, Step}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

class DynamoDBService(dynamoDBClient: AmazonDynamoDB, ingredientsTable: String, recipesTable: String)
                     (implicit ec: ExecutionContext) {

  def getIngredients(lastEvaluatedId: Option[String] = None, limit: Int = 50): Future[(Seq[Ingredient], Option[String])] = {
    val scanRequest = new ScanRequest()
      .withTableName(ingredientsTable)
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
        .withTableName(ingredientsTable)
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

  def getRecipes(lastEvaluatedId: Option[String] = None, limit: Int = 50): Future[(Seq[Recipe], Option[String])] = {
    val scanRequest = new ScanRequest()
      .withTableName(recipesTable)
      .withLimit(limit)

    // Add pagination support with lastEvaluatedId
    lastEvaluatedId.foreach { id =>
      scanRequest.withExclusiveStartKey(Map("id" -> new AttributeValue().withS(id)).asJava)
    }

    Future {
      val result: ScanResult = dynamoDBClient.scan(scanRequest)

      // Convert the result into a sequence of Recipe case class instances
      val recipes: Seq[Recipe] = result.getItems.asScala.map { item =>
        // Convert ingredients list
        val ingredients: Seq[IngredientPerPerson] = item.get("ingredientsPerPerson").getL.asScala.map { ingredient =>
          val map = ingredient.getM.asScala
          IngredientPerPerson(
            id = map("id").getS,
            amount = map("amount").getN.toDouble,
            unit = map("unit").getS
          )
        }.toSeq

        // Convert steps list
        val steps: Seq[Step] = item.get("steps").getL.asScala.map { step =>
          val map = step.getM.asScala
          Step(
            index = map("index").getN.toInt,
            instructions = map("instructions").getS,
            instructionsHTML = map("instructionsHTML").getS
          )
        }.toSeq

        // Convert macros map
        val macrosMap = item.get("macros").getM.asScala
        val macros = Macros(
          fats = macrosMap("fats").getN.toDouble,
          carbs = macrosMap("carbs").getN.toDouble,
          proteins = macrosMap("proteins").getN.toDouble
        )

        // Build Recipe
        Recipe(
          id = item.get("id").getS,
          name = item.get("name").getS,
          totalTime = item.get("totalTime").getS,
          websiteUrl = item.get("websiteUrl").getS,
          imagePath = Option(item.get("imagePath")).map(_.getS).filter(_.nonEmpty),
          cardLink = Option(item.get("pdfPath")).map(_.getS).filter(_.nonEmpty),
          macros = macros,
          ingredients = ingredients,
          steps = steps
        )
      }.toSeq

      // Extract the last evaluated key for pagination
      val lastEvaluatedId: Option[String] = Option(result.getLastEvaluatedKey).flatMap { keyMap =>
        Option(keyMap.get("id")).map(_.getS)
      }

      // Return recipes and the lastEvaluatedId (for pagination)
      (recipes, lastEvaluatedId)
    }.recover {
      case ex: Exception =>
        throw new RuntimeException(s"Error fetching recipes: ${ex.getMessage}")
    }
  }

  def getRecipe(id: String): Future[Option[Recipe]] = {
    Future {
      val getItemRequest = new GetItemRequest()
        .withTableName(recipesTable)
        .withKey(Map("id" -> new AttributeValue().withS(id)).asJava)

      val result = dynamoDBClient.getItem(getItemRequest)

      Option(result.getItem).map { item =>
        // Convert ingredients list
        val ingredients: Seq[IngredientPerPerson] = item.get("ingredientsPerPerson").getL.asScala.map { ingredient =>
          val map = ingredient.getM.asScala
          IngredientPerPerson(
            id = map("id").getS,
            amount = map("amount").getN.toDouble,
            unit = map("unit").getS
          )
        }.toSeq

        // Convert steps list
        val steps: Seq[Step] = item.get("steps").getL.asScala.map { step =>
          val map = step.getM.asScala
          Step(
            index = map("index").getN.toInt,
            instructions = map("instructions").getS,
            instructionsHTML = map("instructionsHTML").getS
          )
        }.toSeq

        // Convert macros map
        val macrosMap = item.get("macros").getM.asScala
        val macros = Macros(
          fats = macrosMap("fats").getN.toDouble,
          carbs = macrosMap("carbs").getN.toDouble,
          proteins = macrosMap("proteins").getN.toDouble
        )

        // Build and return Recipe
        Recipe(
          id = item.get("id").getS,
          name = item.get("name").getS,
          totalTime = item.get("totalTime").getS,
          websiteUrl = item.get("websiteUrl").getS,
          imagePath = Option(item.get("imagePath")).map(_.getS).filter(_.nonEmpty),
          cardLink = Option(item.get("pdfPath")).map(_.getS).filter(_.nonEmpty),
          macros = macros,
          ingredients = ingredients,
          steps = steps
        )
      }
    }.recover {
      case ex: Exception =>
        throw new RuntimeException(s"Error fetching recipe $id: ${ex.getMessage}")
    }
  }
}
