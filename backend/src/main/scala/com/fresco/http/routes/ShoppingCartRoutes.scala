package com.fresco.http.routes

import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.fresco.domain.models.ShoppingCart
import com.fresco.http.formats.JsonFormats
import com.fresco.registries.ShoppingCartRegistry.{GetShoppingCart, GetShoppingCartResponse, PutShoppingCart}
import com.fresco.registries.ShoppingCartRegistry
import com.fresco.http.auth.CognitoAuth

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

// ShoppingCartRoutes class
class ShoppingCartRoutes(cognitoAuth: CognitoAuth, shoppingCartRegistry: ActorRef[ShoppingCartRegistry.Command])(implicit val system: ActorSystem[_]) {

  import JsonFormats.*
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*

  implicit val ec: ExecutionContext = system.executionContext

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("fresco.routes.ask-timeout"))

  // All routes for shopping cart
  val shoppingCartRoutes: Route =
    pathPrefix("api" / "shopping-cart") {
      extractRequestContext { ctx =>
        val tokenOpt = ctx.request.headers.find(_.name() == "Authorization").map(_.value().stripPrefix("Bearer "))
        tokenOpt match {
          case Some(token) =>
            onComplete(cognitoAuth.validateToken(token)(system.classicSystem)) {
              case Success(Some(userId)) => // User is authenticated
                concat(
                  pathEnd {
                    get {
                      onComplete(getShoppingCart(userId)) {
                        case Success(GetShoppingCartResponse(Some(cart))) =>
                          complete(cart)
                        case Success(_) =>
                          complete(StatusCodes.NotFound -> s"Shopping cart for user ID $userId not found")
                        case Failure(exception) =>
                          complete(StatusCodes.InternalServerError -> s"Failed to fetch shopping cart: ${exception.getMessage}")
                      }
                    } ~
                    put {
                      entity(as[ShoppingCart]) { shoppingCart =>
                        onComplete(putShoppingCart(userId, shoppingCart)) {
                          case Success(_) =>
                            complete(StatusCodes.OK -> s"Shopping cart for user ID $userId updated successfully")
                          case Failure(exception) =>
                            complete(StatusCodes.InternalServerError -> s"Failed to update shopping cart: ${exception.getMessage}")
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

  // Method to get the shopping cart for a specific user
  private def getShoppingCart(userId: String): Future[GetShoppingCartResponse] =
    shoppingCartRegistry.ask(GetShoppingCart(userId, _))

  // Method to put (overwrite) the shopping cart for a specific user
  private def putShoppingCart(userId: String, shoppingCart: ShoppingCart): Future[Unit] =
    shoppingCartRegistry.ask(PutShoppingCart(userId, shoppingCart, _))
}
