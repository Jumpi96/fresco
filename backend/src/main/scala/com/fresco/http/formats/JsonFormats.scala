package com.fresco.http.formats

import com.fresco.domain.models.{Ingredient, IngredientPerPerson, Macros, Recipe, Step, ShoppingCart}
import com.fresco.registries.IngredientRegistry.GetIngredientsResponse
import com.fresco.registries.RecipeRegistry.{AddFavouriteRequest, GetRecipeResponse, GetRecipesResponse}

//#json-formats
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol.*

  implicit val addFavouriteRequestFormat: RootJsonFormat[AddFavouriteRequest] = jsonFormat2(AddFavouriteRequest.apply)

  implicit val ingredientJsonFormat: RootJsonFormat[Ingredient] = jsonFormat3(Ingredient.apply)
  implicit val getIngredientsResponseFormat: RootJsonFormat[GetIngredientsResponse] = jsonFormat2(GetIngredientsResponse.apply)

  implicit val ingredientPerPersonFormat: RootJsonFormat[IngredientPerPerson] = jsonFormat3(IngredientPerPerson.apply)
  implicit val stepJsonFormat: RootJsonFormat[Step] = jsonFormat3(Step.apply)
  implicit val macrosJsonFormat: RootJsonFormat[Macros] = jsonFormat3(Macros.apply)
  implicit val recipeJsonFormat: RootJsonFormat[Recipe] = jsonFormat9(Recipe.apply)
  implicit val shoppingCartFormat: RootJsonFormat[ShoppingCart] = jsonFormat2(ShoppingCart.apply)
  implicit val getRecipesResponseFormat: RootJsonFormat[GetRecipesResponse] = jsonFormat2(GetRecipesResponse.apply)
  implicit val getRecipeResponseFormat: RootJsonFormat[GetRecipeResponse] = jsonFormat2(GetRecipeResponse.apply)
}
//#json-formats
