package com.fresco.http.routes

import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestDuration
import com.fresco.domain.models.{IngredientPerPerson, Macros, Recipe, Step}
import com.fresco.http.formats.JsonFormats.*
import com.fresco.registries.RecipeRegistry
import com.fresco.registries.RecipeRegistry.{GetRecipe, GetRecipeResponse, GetRecipes, GetRecipesResponse}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.*

class RecipeRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  // Akka TestKit for Actor-based testing
  val testKit = ActorTestKit()

  implicit def default(implicit system: ActorSystem): RouteTestTimeout =
    RouteTestTimeout(new DurationInt(3).second.dilated(system))

  // Create a TestProbe for simulating ActorRef responses
  val recipeRegistryProbe: TestProbe[RecipeRegistry.Command] = testKit.createTestProbe[RecipeRegistry.Command]()
  val recipeRoutes = new RecipeRoutes(recipeRegistryProbe.ref)(testKit.system)

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
      // Mock a response for GetRecipes from the actor
      val recipes = List(recipeExampleOne,
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

      val test = Get("/recipes?pageSize=2") ~> Route.seal(recipeRoutes.recipeRoutes)
      // In the actual actor, we send a GetRecipes message to the probe and expect it to return a response
      val message = recipeRegistryProbe.expectMessageType[GetRecipes]
      // Capture the `replyTo` ActorRef and send it the mocked response
      message.replyTo ! getRecipesResponse

      test ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[GetRecipesResponse].recipes shouldEqual recipes
      }
    }
    "return a specific recipe when found" in {
      val recipe = recipeExampleOne
      val getRecipeResponse = GetRecipeResponse(Some(recipe))

      // Test the route
      val test = Get("/recipes/1") ~> Route.seal(recipeRoutes.recipeRoutes)
      // Send a GetRecipe message and expect a response from the probe
      val message = recipeRegistryProbe.expectMessageType[GetRecipe]
      message.replyTo ! getRecipeResponse

      test ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Recipe] shouldEqual recipe
      }
    }

    "return 404 when recipe is not found" in {
      val getRecipeResponse = GetRecipeResponse(None)

      // Test the route
      val test = Get("/recipes/999") ~> Route.seal(recipeRoutes.recipeRoutes)
      // Send a GetRecipe message and expect a response from the probe
      val message = recipeRegistryProbe.expectMessageType[GetRecipe]
      message.replyTo ! getRecipeResponse

      test ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[String] shouldEqual "Recipe with ID 999 not found"
      }
    }
  }
}
