package com.fresco.http.routes

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.util.Timeout
import com.fresco.domain.models.{IngredientPerPerson, Macros, Recipe, Step}
import com.fresco.http.auth.CognitoAuth
import com.fresco.http.formats.JsonFormats.*
import com.fresco.registries.RecipeRegistry
import com.fresco.registries.RecipeRegistry.{GetRecipe, GetRecipeResponse, GetRecipes, GetRecipesResponse}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future
import scala.concurrent.duration.*

class RecipeRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest with MockitoSugar {
  val testKit = ActorTestKit()
  // Use untyped ActorSystem for compatibility with ScalatestRouteTest
  override implicit val system: akka.actor.ActorSystem = testKit.system.classicSystem

  implicit def default(implicit system: akka.actor.ActorSystem): RouteTestTimeout =
    RouteTestTimeout(3.seconds)

  val recipeRegistryProbe: TestProbe[RecipeRegistry.Command] = testKit.createTestProbe[RecipeRegistry.Command]()
  val mockCognitoAuth: CognitoAuth = mock[CognitoAuth]
  val recipeRoutes = new RecipeRoutes(mockCognitoAuth, recipeRegistryProbe.ref)(testKit.system)

  val recipeExampleOne: Recipe = Recipe(
    id = "1",
    name = "Recipe1",
    totalTime = "30 minutes",
    websiteUrl = "https://example.com/recipe1",
    imagePath = None,
    cardLink = None,
    macros = Macros(fats = 10.0, carbs = 20.0, proteins = 15.0),
    ingredients = Seq(
      IngredientPerPerson(id = "ing1", amount = 100.0, unit = "g")
    ),
    steps = Seq(
      Step(index = 1, instructions = "Mix ingredients", instructionsHTML = "<p>Mix ingredients</p>")
    )
  )

  "RecipeRoutes" should {
    "return all recipes successfully" in {
      val recipes = List(
        recipeExampleOne,
        Recipe(
          id = "2",
          name = "Recipe2",
          totalTime = "45 minutes",
          websiteUrl = "https://example.com/recipe2",
          imagePath = Some("path"),
          cardLink = Some("https://example.com/recipe2-card"),
          macros = Macros(fats = 15.0, carbs = 25.0, proteins = 20.0),
          ingredients = Seq(
            IngredientPerPerson(id = "ing2", amount = 200.0, unit = "ml")
          ),
          steps = Seq(
            Step(index = 1, instructions = "Prepare ingredients", instructionsHTML = "<p>Prepare ingredients</p>"),
            Step(index = 2, instructions = "Cook", instructionsHTML = "<p>Cook</p>")
          )
        )
      )
      val getRecipesResponse = GetRecipesResponse(recipes, None)

      when(mockCognitoAuth.validateToken(ArgumentMatchers.any[String])(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("userId")))

      val test = Get("/api/recipes?pageSize=2").withHeaders(
        Authorization(akka.http.scaladsl.model.headers.OAuth2BearerToken("someValidToken"))
      ) ~> Route.seal(recipeRoutes.recipeRoutes)

      val message = recipeRegistryProbe.expectMessageType[GetRecipes]
      message.replyTo ! getRecipesResponse

      test ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[GetRecipesResponse].recipes shouldEqual recipes
      }
    }

    "return a specific recipe when found" in {
      val recipeExample = recipeExampleOne
      val getRecipeResponse = GetRecipeResponse(Some(recipeExample), Some(true))

      when(mockCognitoAuth.validateToken(ArgumentMatchers.any[String])(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("userId")))

      val test = Get("/api/recipes/1").withHeaders(
        Authorization(akka.http.scaladsl.model.headers.OAuth2BearerToken("someValidToken"))
      ) ~> Route.seal(recipeRoutes.recipeRoutes)

      // Send a GetRecipe message and expect a response from the probe
      val message = recipeRegistryProbe.expectMessageType[GetRecipe]
      message.replyTo ! getRecipeResponse

      test ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[GetRecipeResponse].recipe.get shouldEqual recipeExample
        responseAs[GetRecipeResponse].isFavourite.get shouldEqual true
      }
    }
  }
}
