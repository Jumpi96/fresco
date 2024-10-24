package com.fresco.http.routes

import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.fresco.http.formats.JsonFormats
import com.fresco.registries.IngredientRegistry.{GetIngredient, GetIngredientResponse, GetIngredientsResponse}
import com.fresco.registries.IngredientRegistry

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

//#import-json-formats
//#ingredient-routes-class
class IngredientRoutes(ingredientRegistry: ActorRef[IngredientRegistry.Command])(implicit val system: ActorSystem[_]) {

  //#ingredient-routes-class
  import JsonFormats.*
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
  //#import-json-formats

  implicit val ec: ExecutionContext = system.executionContext

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("fresco.routes.ask-timeout"))

  def getIngredient(id: String): Future[GetIngredientResponse] =
    ingredientRegistry.ask(GetIngredient(id, _))

  //#all-routes
  val ingredientRoutes: Route =
    pathPrefix("api" / "ingredients") {
      concat(
        pathEnd {
          parameters("pageSize".as[Int].optional, "lastEvaluatedId".optional) { (pageSize, lastEvaluatedId) =>
            val size = pageSize.getOrElse(50)
            val futureIngredients: Future[IngredientRegistry.GetIngredientsResponse] =
              ingredientRegistry.ask(replyTo => IngredientRegistry.GetIngredients(size, lastEvaluatedId, replyTo))

            onComplete(futureIngredients) {
              case Success(GetIngredientsResponse(ingredients, lastEvaluatedId)) =>
                complete(GetIngredientsResponse(ingredients, lastEvaluatedId))
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError -> s"Failed to fetch ingredients: ${ex.getMessage}")
            }
          }
        },
        path(Segment) { id =>
          get {
            onComplete(ingredientRegistry.ask(replyTo => GetIngredient(id, replyTo))) {
              case Success(GetIngredientResponse(Some(ingredient))) =>
                complete(ingredient)
              case Success(_) =>
                complete(StatusCodes.NotFound -> s"Ingredient with ID $id not found")
              case Failure(exception) =>
                complete(StatusCodes.InternalServerError -> s"Failed to fetch ingredient: ${exception.getMessage}")
            }
          }
        })
      }
    }
  //#all-routes
