package com.fresco

//#ingredient-registry-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable

//#ingredient-case-classes
final case class Ingredient(id: String, name: String, imagePath: String)
final case class Ingredients(ingredients: immutable.Seq[Ingredient])
//#ingredient-case-classes

object IngredientRegistry {
  // actor protocol
  sealed trait Command
  final case class GetIngredients(replyTo: ActorRef[Ingredients]) extends Command
  final case class GetIngredient(id: String, replyTo: ActorRef[GetIngredientResponse]) extends Command

  final case class GetIngredientResponse(maybeIngredient: Option[Ingredient])

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(ingredients: Set[Ingredient]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetIngredients(replyTo) =>
        replyTo ! Ingredients(ingredients.toSeq)
        Behaviors.same
      case GetIngredient(id, replyTo) =>
        replyTo ! GetIngredientResponse(ingredients.find(_.id == id))
        Behaviors.same
    }
}
//#ingredient-registry-actor
