package com.fresco

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.fresco.IngredientRegistry.{GetIngredient, GetIngredientResponse, GetIngredients}

import scala.concurrent.Future

//#import-json-formats
//#ingredient-routes-class
class IngredientRoutes(ingredientRegistry: ActorRef[IngredientRegistry.Command])(implicit val system: ActorSystem[_]) {

  //#ingredient-routes-class
  import JsonFormats.*
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
  //#import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("fresco.routes.ask-timeout"))

  def getIngredients(): Future[Ingredients] =
    ingredientRegistry.ask(GetIngredients.apply)
  def getIngredient(id: String): Future[GetIngredientResponse] =
    ingredientRegistry.ask(GetIngredient(id, _))

  //#all-routes
  //#ingredients-get
  //#ingredients-get
  val ingredientRoutes: Route =
    pathPrefix("ingredients") {
      concat(
        //#ingredients-get
        pathEnd {
          concat(
            get {
              complete(getIngredients())
            }
          )
        },
        //#ingredients-get
        //#ingredients-get
        path(Segment) { id =>
          concat(
            get {
              //#retrieve-ingredient-info
              rejectEmptyResponse {
                onSuccess(getIngredient(id)) { response =>
                  complete(response.maybeIngredient)
                }
              }
              //#retrieve-ingredient-info
            })
        })
      //#ingredients-get-delete
    }
  //#all-routes
}
