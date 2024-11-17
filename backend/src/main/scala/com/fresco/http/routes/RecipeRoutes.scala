package com.fresco.http.routes

import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.fresco.http.formats.JsonFormats
import com.fresco.registries.RecipeRegistry
import com.fresco.registries.RecipeRegistry.{AddFavouriteRequest, AddFavouriteResponse, GetRecipesResponse, RemoveFavouriteResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import com.fresco.http.auth.CognitoAuth

//#import-json-formats
//#recipe-routes-class
class RecipeRoutes(cognitoAuth: CognitoAuth, recipeRegistry: ActorRef[RecipeRegistry.Command])(implicit val system: ActorSystem[_]) {

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
      extractRequestContext { ctx =>
        val tokenOpt = ctx.request.headers.find(_.name() == "Authorization").map(_.value().stripPrefix("Bearer "))
        tokenOpt match {
          case Some(token) =>
            onComplete(cognitoAuth.validateToken(token)(system.classicSystem)) {
              case Success(Some(userId)) => // User is authenticated
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
                  }
                )
              case Success(None) =>
                complete(StatusCodes.Unauthorized -> "Invalid token")
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError -> s"Token validation failed: ${ex.getMessage}")
            }
          case None =>
            complete(StatusCodes.Unauthorized -> "Missing token")
        }
      }
    }

  val favouriteRoutes: Route =
    pathPrefix("api" / "favourites") {
      extractRequestContext { ctx =>
        val tokenOpt = ctx.request.headers.find(_.name() == "Authorization").map(_.value().stripPrefix("Bearer "))
        tokenOpt match {
          case Some(token) =>
            onComplete(cognitoAuth.validateToken(token)(system.classicSystem)) {
              case Success(Some(userId)) => // User is authenticated
                concat(
                  // GET method for fetching favourite recipes
                  get {
                    parameters("pageSize".as[Int].optional, "lastEvaluatedId".optional) { (pageSize, lastEvaluatedId) =>
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
                      // Use the userId extracted from the token
                      val futureResponse: Future[RecipeRegistry.AddFavouriteResponse] =
                        recipeRegistry.ask(replyTo => RecipeRegistry.AddFavouriteRecipe(userId, request.recipeId, replyTo))

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
                    parameters("recipeId".as[String]) { recipeId =>
                      // Use the userId extracted from the token
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
              case Success(None) =>
                complete(StatusCodes.Unauthorized -> "Invalid token")
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError -> s"Token validation failed: ${ex.getMessage}")
            }
          case None =>
            complete(StatusCodes.Unauthorized -> "Missing token")
        }
      }
    }

  val recipeRoutes = concat(allRecipeRoutes, favouriteRoutes)
  //#all-routes
}
