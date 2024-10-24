package com.fresco.http.routes

import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.fresco.http.formats.JsonFormats
import com.fresco.registries.RecipeRegistry
import com.fresco.registries.RecipeRegistry.{GetRecipe, GetRecipeResponse, GetRecipesResponse}

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

  def getRecipe(id: String): Future[GetRecipeResponse] =
    recipeRegistry.ask(GetRecipe(id, _))

  //#all-routes
  //#recipes-get
  //#recipes-get
  val recipeRoutes: Route =
    pathPrefix("api" / "recipes") {
      concat(
        pathEnd {
          parameters("pageSize".as[Int].optional, "lastEvaluatedId".optional) { (pageSize, lastEvaluatedId) =>
            val size = pageSize.getOrElse(50)
            val futureRecipes: Future[RecipeRegistry.GetRecipesResponse] =
              recipeRegistry.ask(replyTo => RecipeRegistry.GetRecipes(size, lastEvaluatedId, replyTo))

            onComplete(futureRecipes) {
              case Success(GetRecipesResponse(recipes, lastEvaluatedId)) =>
                complete(GetRecipesResponse(recipes, lastEvaluatedId))
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError -> s"Failed to fetch recipes: ${ex.getMessage}")
            }
          }
        },
        //#recipes-get
        //#recipes-get
        path(Segment) { id =>
          get {
            onComplete(recipeRegistry.ask(replyTo => GetRecipe(id, replyTo))) {
              case Success(GetRecipeResponse(Some(recipe))) =>
                complete(recipe)
              case Success(_) =>
                complete(StatusCodes.NotFound -> s"Recipe with ID $id not found")
              case Failure(exception) =>
                complete(StatusCodes.InternalServerError -> s"Failed to fetch recipe: ${exception.getMessage}")
            }
          }
        })
      //#recipes-get-delete
    }
  //#all-routes
}
