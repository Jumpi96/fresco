package com.fresco.app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import com.fresco.config.DynamoDBClientProvider
import com.fresco.domain.services.DynamoDBService
import com.fresco.http.routes.{IngredientRoutes, UserRoutes}
import com.fresco.registries.{IngredientRegistry, UserRegistry}
import com.typesafe.config.{Config, ConfigFactory}

import scala.util.{Failure, Success}

//#main-class
object FrescoApp {
  //#start-http-server
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
  //#start-http-server
  def main(args: Array[String]): Unit = {
    //#server-bootstrapping
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val config: Config = ConfigFactory.load()
      val environment: String = config.getString("fresco.env")

      val userRegistryActor = context.spawn(UserRegistry(), "UserRegistryActor")
      context.watch(userRegistryActor)

      val awsConfig = config.getConfig(s"fresco.$environment.aws")
      val dynamoDBClient = DynamoDBClientProvider.createDynamoDBClient(awsConfig)
      val ingredientsTable = awsConfig.getConfig("storage").getString("ingredientsTableName")
      val dynamoDBService = DynamoDBService(dynamoDBClient, ingredientsTable)(context.executionContext)
      val ingredientRegistryActor = context.spawn(IngredientRegistry(dynamoDBService), "IngredientRegistryActor")
      context.watch(ingredientRegistryActor)

      val routes: Route = concat(
        new UserRoutes(userRegistryActor)(context.system).userRoutes,
        new IngredientRoutes(ingredientRegistryActor)(context.system).ingredientRoutes
      )
      startHttpServer(routes)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
    //#server-bootstrapping
  }
}
//#main-class
