package com.fresco.app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.{DELETE, GET, OPTIONS, POST, PUT}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers.{HttpOriginRange, `Access-Control-Allow-Credentials`, `Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Origin`}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.{Directive0, Route}
import com.fresco.config.AWSClientsProvider
import com.fresco.domain.repositories.{IngredientRepository, RecipeRepository}
import com.fresco.domain.services.{DynamoDBService, S3Service}
import com.fresco.http.routes.{IngredientRoutes, RecipeRoutes, UserRoutes}
import com.fresco.registries.{IngredientRegistry, RecipeRegistry, UserRegistry}
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
      val ingredientsTable = awsConfig.getConfig("storage").getString("ingredientsTableName")
      val recipesTable = awsConfig.getConfig("storage").getString("recipesTableName")
      val bucketName = awsConfig.getConfig("storage").getString("bucketName")
      val s3Client = AWSClientsProvider.createS3PresignedUrlClient(awsConfig)
      val dynamoDBClient = AWSClientsProvider.createDynamoDBClient(awsConfig)

      val dynamoDBService = DynamoDBService(dynamoDBClient, ingredientsTable, recipesTable)(context.executionContext)
      val s3Service = S3Service(s3Client, bucketName)(context.executionContext)

      val ingredientRepository = IngredientRepository(dynamoDBService, s3Service)(context.executionContext)
      val recipeRepository = RecipeRepository(dynamoDBService, s3Service)(context.executionContext)

      val ingredientRegistryActor = context.spawn(IngredientRegistry(ingredientRepository), "IngredientRegistryActor")
      context.watch(ingredientRegistryActor)
      val recipeRegistryActor = context.spawn(RecipeRegistry(recipeRepository), "RecipeRegistryActor")
      context.watch(recipeRegistryActor)

      val routes: Route = cors() {
        concat(
          new UserRoutes(userRegistryActor)(context.system).userRoutes,
          new IngredientRoutes(ingredientRegistryActor)(context.system).ingredientRoutes,
          new RecipeRoutes(recipeRegistryActor)(context.system).recipeRoutes,
        )
      }
      startHttpServer(routes)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
    //#server-bootstrapping
  }
}
//#main-class
