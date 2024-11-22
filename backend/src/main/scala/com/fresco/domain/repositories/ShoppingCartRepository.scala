package com.fresco.domain.repositories

import scala.concurrent.{ExecutionContext, Future}
import com.fresco.domain.models.ShoppingCart
import com.fresco.domain.services.DynamoDBService

class ShoppingCartRepository(dynamoDBService: DynamoDBService)(implicit ec: ExecutionContext) {

  // Method to get the shopping cart for a specific user
  def getShoppingCart(userId: String): Future[Option[ShoppingCart]] = {
    dynamoDBService.getShoppingCart(userId).flatMap {
      case Some(cartData) =>
        Future.successful(Some(cartData))
      case None =>
        Future.successful(None) // Return None if no cart is found
    }
  }

  // Method to put (overwrite) the shopping cart for a specific user
  def putShoppingCart(userId: String, shoppingCart: ShoppingCart): Future[Unit] = {
    dynamoDBService.putShoppingCart(userId, shoppingCart).map(_ => ())
  }
}