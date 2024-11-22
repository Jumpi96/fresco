package com.fresco.domain.models

final case class ShoppingCart(
  recipes: Map[String, Int],
  shoppedIngredients: Seq[String]
)