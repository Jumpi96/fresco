package com.fresco.domain.repositories

import com.fresco.domain.models.Recipe
import com.fresco.domain.services.{DynamoDBService, S3Service}

import scala.concurrent.{ExecutionContext, Future}

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
}

