package com.fresco.domain.models

final case class IngredientPerPerson(id: String, amount: Double, unit: String)
final case class Step(index: Int, instructions: String, instructionsHTML: String)
final case class Macros(fats: Double, carbs: Double, proteins: Double)
final case class Recipe(
   id: String,
   name: String,
   totalTime: String,
   websiteUrl: String,
   imagePath: Option[String],
   cardLink: Option[String],
   macros: Macros,
   ingredients: Seq[IngredientPerPerson],
   steps: Seq[Step]
 )