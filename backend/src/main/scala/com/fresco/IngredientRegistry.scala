package com.fresco

//#ingredient-registry-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.collection.immutable
import scala.util.{Failure, Success}

//#ingredient-case-classes
final case class Ingredient(id: String, name: String, imagePath: Option[String])
final case class Ingredients(ingredients: immutable.Seq[Ingredient])
//#ingredient-case-classes

object IngredientRegistry {
  // actor protocol
  sealed trait Command
  final case class GetIngredients(replyTo: ActorRef[Ingredients]) extends Command
  final case class GetIngredient(id: String, replyTo: ActorRef[GetIngredientResponse]) extends Command

  final case class LoadIngredientsFromDB() extends Command
  final case class IngredientsLoaded(ingredients: Set[Ingredient]) extends Command
  final case class LoadIngredientsFailed(throwable: Throwable) extends Command

  final case class GetIngredientResponse(maybeIngredient: Option[Ingredient])
  //final case class Ingredients(ingredients: Seq[Ingredient])

  def apply(dynamoDBService: DynamoDBService): Behavior[Command] = registry(Set.empty, dynamoDBService)

  private def registry(ingredients: Set[Ingredient], dynamoDBService: DynamoDBService): Behavior[Command] =
    Behaviors.setup { context =>
      // Optionally load ingredients from DynamoDB when the actor starts
      context.pipeToSelf(dynamoDBService.getIngredients()) {
        case Success(loadedIngredients) => IngredientsLoaded(loadedIngredients.toSet)
        case Failure(ex) => LoadIngredientsFailed(ex)
      }
      Behaviors.receiveMessage {
        case GetIngredients(replyTo) =>
          replyTo ! Ingredients(ingredients.toSeq)
          Behaviors.same
        case GetIngredient(id, replyTo) =>
          replyTo ! GetIngredientResponse(ingredients.find(_.id == id))
          Behaviors.same
        case LoadIngredientsFromDB() =>
          context.pipeToSelf(dynamoDBService.getIngredients()) {
            case Success(loadedIngredients) =>
              IngredientsLoaded(ingredients)
            case Failure(ex) =>
              LoadIngredientsFailed(ex)
          }
          Behaviors.same
        case IngredientsLoaded(ingredients) =>
          registry(ingredients, dynamoDBService)
        case LoadIngredientsFailed(reason) =>
          context.log.error(s"Failed to load ingredients: $reason")
          Behaviors.same
      }
    }
}
//#ingredient-registry-actor
