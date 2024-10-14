package com.fresco.http.formats

import com.fresco.domain.models.{Ingredient, IngredientPerPerson, Macros, Recipe, Step}
import com.fresco.registries.UserRegistry.ActionPerformed
import com.fresco.registries.{User, Users}
import com.fresco.registries.IngredientRegistry.GetIngredientsResponse
import com.fresco.registries.RecipeRegistry.GetRecipesResponse

//#json-formats
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol.*

  implicit val userJsonFormat: RootJsonFormat[User] = jsonFormat3(User.apply)
  implicit val usersJsonFormat: RootJsonFormat[Users] = jsonFormat1(Users.apply)

  implicit val ingredientJsonFormat: RootJsonFormat[Ingredient] = jsonFormat3(Ingredient.apply)
  implicit val getIngredientsResponseFormat: RootJsonFormat[GetIngredientsResponse] = jsonFormat2(GetIngredientsResponse.apply)

  implicit val ingredientPerPersonFormat: RootJsonFormat[IngredientPerPerson] = jsonFormat3(IngredientPerPerson.apply)
  implicit val stepJsonFormat: RootJsonFormat[Step] = jsonFormat3(Step.apply)
  implicit val macrosJsonFormat: RootJsonFormat[Macros] = jsonFormat3(Macros.apply)
  implicit val recipeJsonFormat: RootJsonFormat[Recipe] = jsonFormat9(Recipe.apply)
  implicit val getRecipesResponseFormat: RootJsonFormat[GetRecipesResponse] = jsonFormat2(GetRecipesResponse.apply)

  implicit val actionPerformedJsonFormat: RootJsonFormat[ActionPerformed]  = jsonFormat1(ActionPerformed.apply)
}
//#json-formats
