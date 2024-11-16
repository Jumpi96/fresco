package com.fresco.http.routes

import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.fresco.http.formats.JsonFormats
import com.fresco.registries.RecipeRegistry
import com.fresco.registries.RecipeRegistry.{AddFavouriteRequest, AddFavouriteResponse, GetRecipe, GetRecipeResponse, GetRecipesResponse, RemoveFavouriteResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

//#import-json-formats
//#recipe-routes-class
class RecipeRoutes(recipeRegistry: ActorRef[RecipeRegistry.Command])(implicit val system: ActorSystem[_]) {

  //#recipe-routes-class
  import JsonFormats.*
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
  //#import-json-formats

  implicit val ec: ExecutionContext = system.executionContext

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("fresco.routes.ask-timeout"))

  //#all-routes
  //#recipes-get
  val allRecipeRoutes: Route =
    pathPrefix("api" / "recipes") {
      concat(
        pathEnd {
          parameters("pageSize".as[Int].optional, "random".as[Boolean].optional, "search".optional) { (pageSize, random, search) =>
            val size = pageSize.getOrElse(50)

            (random, search) match {
              case (Some(true), _) =>
                // Fetch random recipes
                val futureRecipes: Future[RecipeRegistry.GetRecipesResponse] =
                  recipeRegistry.ask(replyTo => RecipeRegistry.GetRandomRecipes(size, replyTo))

                onComplete(futureRecipes) {
                  case Success(GetRecipesResponse(recipes, lastEvaluatedId)) =>
                    complete(GetRecipesResponse(recipes, lastEvaluatedId))
                  case Failure(ex) =>
                    complete(StatusCodes.InternalServerError -> s"Failed to fetch random recipes: ${ex.getMessage}")
                }

              case (_, Some(searchTerm)) =>
                // Fetch recipes by search term
                val futureRecipes: Future[RecipeRegistry.GetRecipesResponse] =
                  recipeRegistry.ask(replyTo => RecipeRegistry.SearchRecipes(searchTerm, size, replyTo))

                onComplete(futureRecipes) {
                  case Success(GetRecipesResponse(recipes, lastEvaluatedId)) =>
                    complete(GetRecipesResponse(recipes, lastEvaluatedId))
                  case Failure(ex) =>
                    complete(StatusCodes.InternalServerError -> s"Failed to fetch recipes by search: ${ex.getMessage}")
                }

              case _ =>
                // Fetch regular recipes
                val futureRecipes: Future[RecipeRegistry.GetRecipesResponse] =
                  recipeRegistry.ask(replyTo => RecipeRegistry.GetRecipes(size, replyTo))

                onComplete(futureRecipes) {
                  case Success(GetRecipesResponse(recipes, lastEvaluatedId)) =>
                    complete(GetRecipesResponse(recipes, lastEvaluatedId))
                  case Failure(ex) =>
                    complete(StatusCodes.InternalServerError -> s"Failed to fetch recipes: ${ex.getMessage}")
                }
            }
          }
        },
        path(Segment) { id =>
          get {
            parameters("userId".as[String]) { (userId) =>
              onComplete(recipeRegistry.ask(replyTo => GetRecipe(id, userId, replyTo))) {
                case Success(GetRecipeResponse(recipe, isFavourite)) =>
                  complete(GetRecipeResponse(recipe, isFavourite))
                case Success(_) =>
                  complete(StatusCodes.NotFound -> s"Recipe with ID $id not found")
                case Failure(exception) =>
                  complete(StatusCodes.InternalServerError -> s"Failed to fetch recipe: ${exception.getMessage}")
              }
            }
          }
        })
      //#recipes-get-delete
    }

  val favouriteRoutes: Route =
    pathPrefix("api" / "favourites") {
      concat(
        // GET method for fetching favourite recipes
        get {
          parameters("userId".as[String], "pageSize".as[Int].optional, "lastEvaluatedId".optional) { (userId, pageSize, lastEvaluatedId) =>
            val size = pageSize.getOrElse(50)
            val futureRecipes: Future[RecipeRegistry.GetRecipesResponse] =
              recipeRegistry.ask(replyTo => RecipeRegistry.GetFavouriteRecipes(userId, size, lastEvaluatedId, replyTo))

            onComplete(futureRecipes) {
              case Success(GetRecipesResponse(recipes, lastEvaluatedId)) =>
                complete(GetRecipesResponse(recipes, lastEvaluatedId))
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError -> s"Failed to fetch favourite recipes: ${ex.getMessage}")
            }
          }
        },
        post {
          entity(as[AddFavouriteRequest]) { request =>
            val futureResponse: Future[RecipeRegistry.AddFavouriteResponse] =
              recipeRegistry.ask(replyTo => RecipeRegistry.AddFavouriteRecipe(request.userId, request.recipeId, replyTo))

            onComplete(futureResponse) {
              case Success(AddFavouriteResponse(true)) =>
                complete(StatusCodes.OK -> "Recipe added to favourites.")
              case Success(AddFavouriteResponse(false)) =>
                complete(StatusCodes.BadRequest -> "Failed to add recipe to favourites.")
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError -> s"Failed to add favourite recipe: ${ex.getMessage}")
            }
          }
        },
        delete {
          parameters("userId".as[String], "recipeId".as[String]) { (userId, recipeId) =>
            val futureResponse: Future[RecipeRegistry.RemoveFavouriteResponse] =
              recipeRegistry.ask(replyTo => RecipeRegistry.RemoveFavouriteRecipe(userId, recipeId, replyTo))

            onComplete(futureResponse) {
              case Success(RemoveFavouriteResponse(success)) =>
                complete(StatusCodes.OK -> "Recipe removed from favourites.")
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError -> s"Failed to remove favourite recipe: ${ex.getMessage}")
            }
          }
        }
      )
    }

  val recipeRoutes = concat(allRecipeRoutes, favouriteRoutes)
  //#all-routes
}
