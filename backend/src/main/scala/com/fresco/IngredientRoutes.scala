package com.fresco

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern.*
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.fresco.IngredientRegistry.{GetIngredient, GetIngredientResponse, GetIngredients}

import scala.concurrent.{ExecutionContext, Future}

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
  //#ingredients-get
  //#ingredients-get
  val ingredientRoutes: Route =
    pathPrefix("ingredients") {
      concat(
        pathEnd {
          parameters("pageSize".as[Int].optional, "lastEvaluatedId".optional) { (pageSize, lastEvaluatedId) =>
            val size = pageSize.getOrElse(50)
            val futureIngredients: Future[IngredientRegistry.GetIngredientsResponse] =
              ingredientRegistry.ask(replyTo => IngredientRegistry.GetIngredients(size, lastEvaluatedId, replyTo))

            onSuccess(futureIngredients) { response =>
              complete(response)
            }
          }
        },
        //#ingredients-get
        //#ingredients-get
        path(Segment) { id =>
          get {
            onSuccess(ingredientRegistry.ask(replyTo => IngredientRegistry.GetIngredient(id, replyTo)).map {
              case IngredientRegistry.GetIngredientResponse(maybeIngredient) => maybeIngredient
            }) {
              case Some(ingredient) => complete(ingredient)
              case None => complete(StatusCodes.NotFound)
            }
          }
        })
      //#ingredients-get-delete
    }
  //#all-routes
}
