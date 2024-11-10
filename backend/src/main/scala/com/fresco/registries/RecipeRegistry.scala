package com.fresco.registries

//#recipe-registry-actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.fresco.domain.models.Recipe
import com.fresco.domain.repositories.RecipeRepository
import org.slf4j.Logger

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scala.concurrent.Future

object RecipeRegistry {
  // actor protocol
  sealed trait Command
  final case class GetRecipes(pageSize: Int, replyTo: ActorRef[GetRecipesResponse]) extends Command
  final case class GetFavouriteRecipes(userId: String, pageSize: Int, lastEvaluatedId: Option[String], replyTo: ActorRef[GetRecipesResponse]) extends Command
  final case class AddFavouriteRecipe(userId: String, recipeId: String, replyTo: ActorRef[AddFavouriteResponse]) extends Command
  final case class GetRecipesResponse(recipes: Seq[Recipe], lastEvaluatedId: Option[String]) extends Command
  final case class GetRecipe(id: String, userId: String, replyTo: ActorRef[GetRecipeResponse]) extends Command
  final case class GetRecipeResponse(recipe: Option[Recipe], isFavourite: Option[Boolean]) extends Command
  final case class AddFavouriteResponse(success: Boolean) extends Command
  final case class AddFavouriteRequest(userId: String, recipeId: String) extends Command
  final case class RemoveFavouriteResponse(success: Boolean) extends Command
  final case class RemoveFavouriteRecipe(userId: String, recipeId: String, replyTo: ActorRef[RemoveFavouriteResponse]) extends Command
  final case class ItemCountResponse(count: Long) extends Command

  // New case class to hold the state of the actor
  final case class State(itemCount: Long)

  def apply(repository: RecipeRepository): Behavior[Command] = {
    Behaviors.setup { context =>
      implicit val ec: ExecutionContext = context.executionContext
      val log = context.log

      // Fetch the item count when the actor starts
      val itemCountFuture: Future[Long] = repository.getRecipeCount

      itemCountFuture.onComplete {
        case Success(count) =>
          log.info(s"Total number of recipes in the table: $count")
          // Start the registry with the item count
          context.self ! ItemCountResponse(count) // Send a message to self to update state
        case Failure(ex) =>
          log.error(s"Failed to get recipe count: ${ex.getMessage}")
          // Start the registry with a default count of 0
          context.self ! ItemCountResponse(0)
      }

      registry(repository, State(0))(log, ec) // Initialize with a default state
    }
  }

  private def registry(repository: RecipeRepository, state: State)(implicit log: Logger, ec: ExecutionContext): Behavior[Command] =
    Behaviors.receiveMessage {
      case ItemCountResponse(count) =>
        // Update the state with the fetched item count
        log.info(s"Item count updated to: $count")
        registry(repository, state.copy(itemCount = count)) // Update state

      case GetRecipes(pageSize, replyTo) =>
        // Fetch recipes with pagination
        repository.getRandomRecipes(state.itemCount, pageSize).onComplete {
          case Success((recipes, newLastEvaluatedId)) =>
            log.info(s"Fetched ${recipes.size} recipes from DynamoDB")
            replyTo ! GetRecipesResponse(recipes, newLastEvaluatedId)
          case Failure(ex) =>
            log.error(s"Failed to fetch recipes: ${ex.getMessage}")
            replyTo ! GetRecipesResponse(Seq.empty, None)
        }
        Behaviors.same

      case GetRecipe(id, userId, replyTo) =>
        log.info(s"Fetching recipe with ID: $id")
        // Check if the recipe is a favourite asynchronously
        val isFavouriteFuture: Future[Boolean] = repository.isFavouriteRecipe(userId, id)

        // Fetch the recipe and handle both futures
        for {
          isFavourite <- isFavouriteFuture
          recipeOpt <- repository.getRecipe(id)
        } yield {
          recipeOpt match {
            case Some(recipe) =>
              log.info(s"Fetched recipe: $id")
              replyTo ! GetRecipeResponse(Some(recipe), Some(isFavourite))
            case None =>
              log.warn(s"Recipe with ID: $id not found")
              replyTo ! GetRecipeResponse(None, None)
          }
        }

        Behaviors.same // Ensure the behavior remains the same

      case GetFavouriteRecipes(userId, pageSize, lastEvaluatedId, replyTo) =>
        // Fetch recipes with pagination
        repository.getFavouriteRecipes(userId, lastEvaluatedId, pageSize).onComplete {
          case Success((recipes, newLastEvaluatedId)) =>
            log.info(s"Fetched ${recipes.size} favourite recipes from DynamoDB")
            replyTo ! GetRecipesResponse(recipes, newLastEvaluatedId)
          case Failure(ex) =>
            log.error(s"Failed to fetch favourite recipes: ${ex.getMessage}")
            replyTo ! GetRecipesResponse(Seq.empty, None)
        }
        Behaviors.same

      case AddFavouriteRecipe(userId, recipeId, replyTo) =>
        // Logic to add a favourite recipe
        repository.addFavouriteRecipe(userId, recipeId).onComplete {
          case Success(success) =>
            log.info(s"Successfully added recipe $recipeId to user $userId's favourites")
            replyTo ! AddFavouriteResponse(success = true)
          case Failure(ex) =>
            log.error(s"Failed to add favourite recipe: ${ex.getMessage}")
            replyTo ! AddFavouriteResponse(success = false)
        }
        Behaviors.same

      case RemoveFavouriteRecipe(userId, recipeId, replyTo) =>
        // Logic to remove a favourite recipe
        repository.removeFavouriteRecipe(userId, recipeId).onComplete {
          case Success(success) =>
            log.info(s"Successfully removed recipe $recipeId from user $userId's favourites")
            replyTo ! RemoveFavouriteResponse(success = true)
          case Failure(ex) =>
            log.error(s"Failed to remove favourite recipe: ${ex.getMessage}")
            replyTo ! RemoveFavouriteResponse(success = false)
        }
        Behaviors.same
    }
}
//#recipe-registry-actor
