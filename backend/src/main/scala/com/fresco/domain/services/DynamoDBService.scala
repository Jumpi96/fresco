package com.fresco.domain.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, DeleteItemRequest, GetItemRequest, PutItemRequest, QueryRequest, QueryResult, ScanRequest, ScanResult}
import com.fresco.domain.models.{Ingredient, IngredientPerPerson, Macros, Recipe, Step}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

class DynamoDBService(dynamoDBClient: AmazonDynamoDB, ingredientsTable: String, recipesTable: String, favouriteRecipesTable: String)
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

  def getRecipeByIndex(index: Long): Future[Option[Recipe]] = {
    Future {
      val request = new QueryRequest()
        .withTableName(recipesTable)
        .withIndexName("IndexNumberIndex") // TODO: move from here
        .withKeyConditionExpression("indexNumber = :indexNumber")
        .addExpressionAttributeValuesEntry(":indexNumber", new AttributeValue().withN(index.toString))

      val result: QueryResult = dynamoDBClient.query(request)
      if (result.getItems.isEmpty) {
        None
      } else {
        Some(convertToRecipe(result.getItems.get(0)))
      }
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

  private def convertToRecipe(item: java.util.Map[String, AttributeValue]): Recipe = {
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
        convertToRecipe(item) // Use the new convertToRecipe method
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
        convertToRecipe(item) // Use the new convertToRecipe method
      }
    }.recover {
      case ex: Exception =>
        throw new RuntimeException(s"Error fetching recipe $id: ${ex.getMessage}")
    }
  }

  def getFavouriteRecipes(userId: String, lastEvaluatedId: Option[String] = None, limit: Int = 50): Future[(Seq[Recipe], Option[String])] = {
    val scanRequest = new ScanRequest()
      .withTableName(favouriteRecipesTable)
      .withLimit(limit)
      .withFilterExpression("userId = :userId")
      .withExpressionAttributeValues(Map(":userId" -> new AttributeValue().withS(userId)).asJava)

    // Add pagination support with lastEvaluatedId
    lastEvaluatedId.foreach { id =>
      scanRequest.withExclusiveStartKey(Map("id" -> new AttributeValue().withS(id)).asJava)
    }

    val result: ScanResult = dynamoDBClient.scan(scanRequest)
    val recipeIds = result.getItems.asScala.map { item =>
      item.get("recipeId").getS
    }

    Future.sequence(recipeIds.map(getRecipe)).map { recipes =>
      val filteredRecipes = recipes.flatten.toSeq
      val lastEvaluatedKey = Option(result.getLastEvaluatedKey).flatMap { keyMap =>
        Option(keyMap.get("id")).map(_.getS)
      }
      (filteredRecipes, lastEvaluatedKey)
    }.recover {
      case ex: Exception =>
        throw new RuntimeException(s"Error fetching recipes: ${ex.getMessage}")
    }
  }

  def addFavouriteRecipe(userId: String, recipeId: String): Future[Boolean] = {
    val item = Map(
      "userId" -> new AttributeValue().withS(userId),
      "recipeId" -> new AttributeValue().withS(recipeId)
    ).asJava

    val putItemRequest = new PutItemRequest()
      .withTableName(favouriteRecipesTable)
      .withItem(item)

    Future {
      dynamoDBClient.putItem(putItemRequest)
      true // Return true if the operation is successful
    }.recover {
      case ex: Exception =>
        throw new RuntimeException(s"Error adding favourite recipe: ${ex.getMessage}")
    }
  }

  def removeFavouriteRecipe(userId: String, recipeId: String): Future[Boolean] = {
    val key = Map(
      "userId" -> new AttributeValue().withS(userId),
      "recipeId" -> new AttributeValue().withS(recipeId)
    ).asJava

    val deleteItemRequest = new DeleteItemRequest()
      .withTableName(favouriteRecipesTable)
      .withKey(key)

    Future {
      dynamoDBClient.deleteItem(deleteItemRequest)
      true // Return true if the operation is successful
    }.recover {
      case ex: Exception =>
        throw new RuntimeException(s"Error deleting favourite recipe: ${ex.getMessage}")
    }
  }

  def isFavouriteRecipe(userId: String, recipeId: String): Future[Boolean] = {
    val key = Map(
      "userId" -> new AttributeValue().withS(userId),
      "recipeId" -> new AttributeValue().withS(recipeId)
    ).asJava

    val getItemRequest = new GetItemRequest()
      .withTableName(favouriteRecipesTable)
      .withKey(key)

    Future {
      val result = dynamoDBClient.getItem(getItemRequest)
      result.getItem != null
    }.recover {
      case ex: Exception =>
        throw new RuntimeException(s"Error checking favourite recipe: ${ex.getMessage}")
    }
  }

  def getRecipeCount: Future[Long] = {
    Future {
      dynamoDBClient.describeTable(recipesTable).getTable.getItemCount
    }
  }

  def searchRecipes(searchTerm: String, limit: Int = 50): Future[(Seq[Recipe], Option[String])] = {
    val scanRequest = new ScanRequest()
      .withTableName(recipesTable)
      .withFilterExpression("contains(#n, :name)") // Use contains in the filter expression
      .addExpressionAttributeNamesEntry("#n", "name") // Map placeholder to actual attribute name
      .addExpressionAttributeValuesEntry(":name", new AttributeValue().withS(searchTerm))

    Future {
      val result: ScanResult = dynamoDBClient.scan(scanRequest)

      // Convert the result into a sequence of Recipe case class instances
      val recipes: Seq[Recipe] = result.getItems.asScala.map { item =>
        convertToRecipe(item) // Use the existing convertToRecipe method
      }.toSeq

      // Limit the number of recipes returned
      val limitedRecipes = recipes.take(limit)

      // Extract the last evaluated key for pagination
      val lastEvaluatedId: Option[String] = Option(result.getLastEvaluatedKey).flatMap { keyMap =>
        Option(keyMap.get("id")).map(_.getS)
      }

      // Return recipes and the lastEvaluatedId (for pagination)
      (limitedRecipes, lastEvaluatedId)
    }.recover {
      case ex: Exception =>
        throw new RuntimeException(s"Error searching recipes: ${ex.getMessage}")
    }
  }
}
