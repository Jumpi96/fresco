package com.fresco.registries

//#recipe-registry-actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.fresco.domain.models.Recipe
import com.fresco.domain.repositories.RecipeRepository
import org.slf4j.Logger

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


object RecipeRegistry {
  // actor protocol
  sealed trait Command
  final case class GetRecipes(pageSize: Int, lastEvaluatedId: Option[String], replyTo: ActorRef[GetRecipesResponse]) extends Command
  final case class GetRecipesResponse(recipes: Seq[Recipe], lastEvaluatedId: Option[String])
  final case class GetRecipe(id: String, replyTo: ActorRef[GetRecipeResponse]) extends Command
  final case class GetRecipeResponse(maybeRecipe: Option[Recipe])

  def apply(repository: RecipeRepository): Behavior[Command] = {
    Behaviors.setup { context =>
      implicit val ec: ExecutionContext = context.executionContext
      val log = context.log

      registry(repository)(log, ec)
    }
  }

  private def registry(repository: RecipeRepository)(implicit log: Logger, ec: ExecutionContext): Behavior[Command] =
    Behaviors.setup { context =>

      Behaviors.receiveMessage {
        case GetRecipes(pageSize, lastEvaluatedId, replyTo) =>
          // Fetch recipes with pagination
          repository.getRecipes(lastEvaluatedId, pageSize).onComplete {
            case Success((recipes, newLastEvaluatedId)) =>
              log.info(s"Fetched ${recipes.size} recipes from DynamoDB")
              replyTo ! GetRecipesResponse(recipes, newLastEvaluatedId)
            case Failure(ex) =>
              // Handle failure case here, e.g. by sending an empty list
              log.error(s"Failed to fetch recipes: ${ex.getMessage}")
              replyTo ! GetRecipesResponse(Seq.empty, None)
          }
          Behaviors.same
        case GetRecipe(id, replyTo) =>
          log.info(s"Fetching recipe with ID: $id")
          repository.getRecipe(id).onComplete {
            case Success(Some(recipe)) =>
              log.info(s"Fetched recipe: $id")
              replyTo ! GetRecipeResponse(Some(recipe))
            case Success(_) =>
              log.warn(s"Recipe with ID: $id not found")
              replyTo ! GetRecipeResponse(None)
            case Failure(ex) =>
              log.error(s"Failed to fetch recipe: ${ex.getMessage}")
              replyTo ! GetRecipeResponse(None)
          }
          Behaviors.same
      }
    }
}
//#recipe-registry-actor
