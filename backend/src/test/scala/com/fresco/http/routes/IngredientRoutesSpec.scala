package com.fresco.http.routes

import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.fresco.domain.models.Ingredient
import com.fresco.registries.IngredientRegistry
import com.fresco.registries.IngredientRegistry.{GetIngredient, GetIngredientResponse, GetIngredients, GetIngredientsResponse}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.testkit.TestDuration
import com.fresco.http.formats.JsonFormats.*

import scala.concurrent.duration.*

class IngredientRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  // Akka TestKit for Actor-based testing
  val testKit = ActorTestKit()

  implicit def default(implicit system: ActorSystem): RouteTestTimeout =
    RouteTestTimeout(new DurationInt(3).second.dilated(system))

  // Create a TestProbe for simulating ActorRef responses
  val ingredientRegistryProbe: TestProbe[IngredientRegistry.Command] = testKit.createTestProbe[IngredientRegistry.Command]()
  val ingredientRoutes = new IngredientRoutes(ingredientRegistryProbe.ref)(testKit.system)

  "IngredientRoutes" should {

    "return all ingredients successfully" in {
      // Mock a response for GetIngredients from the actor
      val ingredients = List(Ingredient("1", "Ingredient1", None), Ingredient("2", "Ingredient2", Some("path")))
      val getIngredientsResponse = GetIngredientsResponse(ingredients, None)

      val test = Get("/api/ingredients?pageSize=2") ~> Route.seal(ingredientRoutes.ingredientRoutes)
      // In the actual actor, we send a GetIngredients message to the probe and expect it to return a response
      val message = ingredientRegistryProbe.expectMessageType[GetIngredients]
      // Capture the `replyTo` ActorRef and send it the mocked response
      message.replyTo ! getIngredientsResponse

      test ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[GetIngredientsResponse].ingredients shouldEqual ingredients
      }
    }
    "return a specific ingredient when found" in {
      val ingredient = Ingredient("1", "Ingredient1", None)
      val getIngredientResponse = GetIngredientResponse(Some(ingredient))

      // Test the route
      val test = Get("/api/ingredients/1") ~> Route.seal(ingredientRoutes.ingredientRoutes)
      // Send a GetIngredient message and expect a response from the probe
      val message = ingredientRegistryProbe.expectMessageType[GetIngredient]
      message.replyTo ! getIngredientResponse

      test ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Ingredient] shouldEqual ingredient
      }
    }

    "return 404 when ingredient is not found" in {
      val getIngredientResponse = GetIngredientResponse(None)

      // Test the route
      val test = Get("/api/ingredients/999") ~> Route.seal(ingredientRoutes.ingredientRoutes)
      // Send a GetIngredient message and expect a response from the probe
      val message = ingredientRegistryProbe.expectMessageType[GetIngredient]
      message.replyTo ! getIngredientResponse

      test ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[String] shouldEqual "Ingredient with ID 999 not found"
      }
    }
  }
}
