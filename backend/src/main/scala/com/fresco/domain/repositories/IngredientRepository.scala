package com.fresco.domain.repositories

import scala.concurrent.{ExecutionContext, Future}
import com.fresco.domain.models.Ingredient
import com.fresco.domain.services.{DynamoDBService, S3Service}

class IngredientRepository(dynamoDBService: DynamoDBService, s3Service: S3Service)(implicit ec: ExecutionContext) {

  def getIngredient(id: String): Future[Option[Ingredient]] = {
    dynamoDBService.getIngredient(id).flatMap {
      case Some(ingredient) =>
        ingredient.imagePath match {
          case Some(imagePath) =>
            s3Service.generatePresignedUrl(imagePath).map { url =>
              Some(ingredient.copy(imagePath = Some(url.toString)))
            }.recover {
              case _ => Some(ingredient.copy(imagePath = None))
            }
          case None =>
            Future.successful(Some(ingredient.copy(imagePath = None)))
        }
      case None => Future.failed(new RuntimeException(s"Ingredient with id $id not found"))
    }
  }

  def getIngredients(lastEvaluatedId: Option[String], pageSize: Int): Future[(Seq[Ingredient], Option[String])] = {
    dynamoDBService.getIngredients(lastEvaluatedId, pageSize).flatMap { case (ingredients, lastEvaluatedKey) =>
      Future.sequence(ingredients.map { ingredient =>
        ingredient.imagePath match {
          case Some(imagePath) =>
            s3Service.generatePresignedUrl(imagePath).map { url =>
              ingredient.copy(imagePath = Some(url.toString))
            }.recover {
              case _ => ingredient.copy(imagePath = None)
            }
          case None =>
            Future.successful(ingredient.copy(imagePath = None))
        }
      }).map(updatedIngredients => (updatedIngredients, lastEvaluatedKey))
    }
  }
}

