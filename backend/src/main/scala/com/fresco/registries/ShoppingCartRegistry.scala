package com.fresco.registries

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.fresco.domain.models.ShoppingCart
import com.fresco.domain.repositories.ShoppingCartRepository
import org.slf4j.Logger

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ShoppingCartRegistry {
  // Actor protocol
  sealed trait Command
  final case class GetShoppingCart(userId: String, replyTo: ActorRef[GetShoppingCartResponse]) extends Command
  final case class GetShoppingCartResponse(maybeCart: Option[ShoppingCart])
  final case class PutShoppingCart(userId: String, shoppingCart: ShoppingCart, replyTo: ActorRef[Unit]) extends Command

  def apply(repository: ShoppingCartRepository): Behavior[Command] = {
    Behaviors.setup { context =>
      implicit val ec: ExecutionContext = context.executionContext
      val log = context.log

      registry(repository)(log, ec)
    }
  }

  private def registry(repository: ShoppingCartRepository)
                      (implicit log: Logger, ec: ExecutionContext): Behavior[Command] =
    Behaviors.setup { context =>

      Behaviors.receiveMessage {
        case GetShoppingCart(userId, replyTo) =>
          log.info(s"Fetching shopping cart for user ID: $userId")
          repository.getShoppingCart(userId).onComplete {
            case Success(Some(cart)) =>
              log.info(s"Fetched shopping cart for user ID: $userId")
              replyTo ! GetShoppingCartResponse(Some(cart))
            case Success(None) =>
              log.warn(s"Shopping cart for user ID: $userId not found")
              replyTo ! GetShoppingCartResponse(None)
            case Failure(ex) =>
              log.error(s"Failed to fetch shopping cart: ${ex.getMessage}")
              replyTo ! GetShoppingCartResponse(None)
          }
          Behaviors.same

        case PutShoppingCart(userId, shoppingCart, replyTo) =>
          log.info(s"Updating shopping cart for user ID: $userId")
          repository.putShoppingCart(userId, shoppingCart).onComplete {
            case Success(_) =>
              log.info(s"Updated shopping cart for user ID: $userId successfully")
              replyTo ! ()
            case Failure(ex) =>
              log.error(s"Failed to update shopping cart: ${ex.getMessage}")
              replyTo ! ()
          }
          Behaviors.same
      }
    }
}
