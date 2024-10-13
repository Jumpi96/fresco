package com.fresco.http.formats

import com.fresco.domain.models.Ingredient
import com.fresco.registries.UserRegistry.ActionPerformed
import com.fresco.registries.{User, Users}
import com.fresco.registries.IngredientRegistry.GetIngredientsResponse

//#json-formats
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol.*

  implicit val userJsonFormat: RootJsonFormat[User] = jsonFormat3(User.apply)
  implicit val usersJsonFormat: RootJsonFormat[Users] = jsonFormat1(Users.apply)

  implicit val ingredientJsonFormat: RootJsonFormat[Ingredient] = jsonFormat3(Ingredient.apply)
  implicit val getIngredientsResponseFormat: RootJsonFormat[GetIngredientsResponse] = jsonFormat2(GetIngredientsResponse.apply)

  implicit val actionPerformedJsonFormat: RootJsonFormat[ActionPerformed]  = jsonFormat1(ActionPerformed.apply)
}
//#json-formats
