package com.fresco

//#ingredient-registry-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

//import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

//#ingredient-case-classes
final case class Ingredient(id: String, name: String, imagePath: Option[String])
//#ingredient-case-classes

object IngredientRegistry {
  // actor protocol
  sealed trait Command
  final case class GetIngredients(pageSize: Int, lastEvaluatedId: Option[String], replyTo: ActorRef[GetIngredientsResponse]) extends Command
  final case class GetIngredientsResponse(ingredients: Seq[Ingredient], lastEvaluatedId: Option[String])
  final case class GetIngredient(id: String, replyTo: ActorRef[GetIngredientResponse]) extends Command
  final case class GetIngredientResponse(maybeIngredient: Option[Ingredient])

  def apply(dynamoDBService: DynamoDBService)(implicit ec: ExecutionContext): Behavior[Command] =
    registry(dynamoDBService)

  private def registry(dynamoDBService: DynamoDBService)(implicit ec: ExecutionContext): Behavior[Command] =
    Behaviors.setup { context =>

      Behaviors.receiveMessage {
        case GetIngredients(pageSize, lastEvaluatedId, replyTo) =>
          // Fetch ingredients with pagination
          dynamoDBService.getIngredients(lastEvaluatedId).onComplete {
            case Success((ingredients, newLastEvaluatedId)) =>
              replyTo ! GetIngredientsResponse(ingredients, newLastEvaluatedId)
            case Failure(_) =>
              // Handle failure case here, e.g. by sending an empty list
              replyTo ! GetIngredientsResponse(Seq.empty, None)
          }
          Behaviors.same
        case GetIngredient(id, replyTo) =>
          dynamoDBService.getIngredient(id).map {
            case Some(ingredient) => replyTo ! GetIngredientResponse(Some(ingredient))
            case None => replyTo ! GetIngredientResponse(None)
          }
          Behaviors.same
      }
    }
}
//#ingredient-registry-actor
