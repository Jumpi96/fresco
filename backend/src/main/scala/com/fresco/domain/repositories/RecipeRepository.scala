package com.fresco.domain.repositories

import com.fresco.domain.models.Recipe
import com.fresco.domain.services.{DynamoDBService, S3Service}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class RecipeRepository(dynamoDBService: DynamoDBService, s3Service: S3Service)(implicit ec: ExecutionContext) {

  def getRecipe(id: String): Future[Option[Recipe]] = {
    dynamoDBService.getRecipe(id).flatMap {
      case Some(recipe) =>
        recipe.imagePath match {
          case Some(imagePath) =>
            s3Service.generatePresignedUrl(imagePath).map { url =>
              Some(recipe.copy(imagePath = Some(url.toString)))
            }.recover {
              case _ => Some(recipe.copy(imagePath = None))
            }
          case None =>
            Future.successful(Some(recipe.copy(imagePath = None)))
        }
      case None => Future.failed(new RuntimeException(s"Recipe with id $id not found"))
    }
  }

  def getRecipeByIndex(index: Long): Future[Option[Recipe]] = {
    dynamoDBService.getRecipeByIndex(index)
  }

  def getRandomRecipes(recipesCount: Long, pageSize: Int): Future[(Seq[Recipe], Option[String])] = {
    if (recipesCount <= 0 || pageSize <= 0) {
      Future.successful(Seq.empty, None) // Return empty if no recipes or page size is invalid
    } else {
      // Generate unique random indices
      val randomIndices = Random.shuffle((0 until recipesCount.toInt).toList).take(pageSize)

      // Fetch recipes based on the random indices
      val recipeFutures: Seq[Future[Option[Recipe]]] = randomIndices.map { index =>
        getRecipeByIndex(index) // Assuming the index can be converted to a recipe ID
      }

      // Combine all futures and process the results
      Future.sequence(recipeFutures).flatMap { recipes =>
        // Process each recipe to generate presigned URLs
        val updatedRecipeFutures: Seq[Future[Recipe]] = recipes.flatten.map { recipe =>
          recipe.imagePath match {
            case Some(imagePath) =>
              s3Service.generatePresignedUrl(imagePath).map { url =>
                recipe.copy(imagePath = Some(url.toString))
              }.recover {
                case _ => recipe.copy(imagePath = None) // Fallback if URL generation fails
              }
            case None =>
              Future.successful(recipe.copy(imagePath = None)) // No image path, return as is
          }
        }

        // Combine all updated recipe futures and return the results
        Future.sequence(updatedRecipeFutures).map { validRecipes =>
          (validRecipes, None) // Return the valid recipes and None for last evaluated key
        }
      }
    }
  }

  def getRecipes(lastEvaluatedId: Option[String], pageSize: Int): Future[(Seq[Recipe], Option[String])] = {
    dynamoDBService.getRecipes(lastEvaluatedId, pageSize).flatMap { case (recipes, lastEvaluatedKey) =>
      Future.sequence(recipes.map { recipe =>
        recipe.imagePath match {
          case Some(imagePath) =>
            s3Service.generatePresignedUrl(imagePath).map { url =>
              recipe.copy(imagePath = Some(url.toString))
            }.recover {
              case _ => recipe.copy(imagePath = None)
            }
          case None =>
            Future.successful(recipe.copy(imagePath = None))
        }
      }).map(updatedRecipes => (updatedRecipes, lastEvaluatedKey))
    }
  }

  def getFavouriteRecipes(userId: String, lastEvaluatedId: Option[String], pageSize: Int): Future[(Seq[Recipe], Option[String])] = {
    dynamoDBService.getFavouriteRecipes(userId, lastEvaluatedId, pageSize).flatMap { case (recipes, lastEvaluatedKey) =>
      Future.sequence(recipes.map { recipe =>
        recipe.imagePath match {
          case Some(imagePath) =>
            s3Service.generatePresignedUrl(imagePath).map { url =>
              recipe.copy(imagePath = Some(url.toString))
            }.recover {
              case _ => recipe.copy(imagePath = None)
            }
          case None =>
            Future.successful(recipe.copy(imagePath = None))
        }
      }).map(updatedRecipes => (updatedRecipes, lastEvaluatedKey))
    }
  }

  def addFavouriteRecipe(userId: String, recipeId: String): Future[Unit] = {
    dynamoDBService.addFavouriteRecipe(userId, recipeId).map(_ => ())
  }

  def removeFavouriteRecipe(userId: String, recipeId: String): Future[Unit] = {
    dynamoDBService.removeFavouriteRecipe(userId, recipeId).map(_ => ())
  }

  def isFavouriteRecipe(userId: String, recipeId: String): Future[Boolean] = {
    dynamoDBService.isFavouriteRecipe(userId, recipeId)
  }

  def getRecipeCount: Future[Long] = {
    dynamoDBService.getRecipeCount
  }
}

