package com.fresco.registries

//#ingredient-registry-actor
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.fresco.domain.models.Ingredient
import com.fresco.domain.services.DynamoDBService
import org.slf4j.Logger

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


object IngredientRegistry {
  // actor protocol
  sealed trait Command
  final case class GetIngredients(pageSize: Int, lastEvaluatedId: Option[String], replyTo: ActorRef[GetIngredientsResponse]) extends Command
  final case class GetIngredientsResponse(ingredients: Seq[Ingredient], lastEvaluatedId: Option[String])
  final case class GetIngredient(id: String, replyTo: ActorRef[GetIngredientResponse]) extends Command
  final case class GetIngredientResponse(maybeIngredient: Option[Ingredient])

  def apply(dynamoDBService: DynamoDBService): Behavior[Command] = {
    Behaviors.setup { context =>
      implicit val ec: ExecutionContext = context.executionContext
      val log = context.log

      registry(dynamoDBService)(log, ec)
    }
  }

  private def registry(dynamoDBService: DynamoDBService)(implicit log: Logger, ec: ExecutionContext): Behavior[Command] =
    Behaviors.setup { context =>

      Behaviors.receiveMessage {
        case GetIngredients(pageSize, lastEvaluatedId, replyTo) =>
          // Fetch ingredients with pagination
          dynamoDBService.getIngredients(lastEvaluatedId, pageSize).onComplete {
            case Success((ingredients, newLastEvaluatedId)) =>
              log.info(s"Fetched ${ingredients.size} ingredients from DynamoDB")
              replyTo ! GetIngredientsResponse(ingredients, newLastEvaluatedId)
            case Failure(ex) =>
              // Handle failure case here, e.g. by sending an empty list
              log.error(s"Failed to fetch ingredients: ${ex.getMessage}")
              replyTo ! GetIngredientsResponse(Seq.empty, None)
          }
          Behaviors.same
        case GetIngredient(id, replyTo) =>
          log.info(s"Fetching ingredient with ID: $id")
          dynamoDBService.getIngredient(id).onComplete {
            case Success(Some(ingredient)) =>
              log.info(s"Fetched ingredient: $id")
              replyTo ! GetIngredientResponse(Some(ingredient))
            case Success(_) =>
              log.warn(s"Ingredient with ID: $id not found")
              replyTo ! GetIngredientResponse(None)
            case Failure(ex) =>
              log.error(s"Failed to fetch ingredient: ${ex.getMessage}")
              replyTo ! GetIngredientResponse(None)
          }
          Behaviors.same
      }
    }
}
//#ingredient-registry-actor
