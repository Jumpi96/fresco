package com.fresco

import com.fresco.IngredientRegistry.GetIngredientsResponse
import com.fresco.UserRegistry.ActionPerformed

//#json-formats
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat: RootJsonFormat[User] = jsonFormat3(User.apply)
  implicit val usersJsonFormat: RootJsonFormat[Users] = jsonFormat1(Users.apply)

  implicit val ingredientJsonFormat: RootJsonFormat[Ingredient] = jsonFormat3(Ingredient.apply)
  implicit val getIngredientsResponseFormat: RootJsonFormat[GetIngredientsResponse] = jsonFormat2(GetIngredientsResponse.apply)

  implicit val actionPerformedJsonFormat: RootJsonFormat[ActionPerformed]  = jsonFormat1(ActionPerformed.apply)
}
//#json-formats
