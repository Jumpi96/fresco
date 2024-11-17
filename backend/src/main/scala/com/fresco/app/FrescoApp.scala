package com.fresco.app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import com.fresco.config.AWSClientsProvider
import com.fresco.domain.repositories.{IngredientRepository, RecipeRepository}
import com.fresco.domain.services.{DynamoDBService, S3Service}
import com.fresco.http.CORSHandler
import com.fresco.http.auth.CognitoAuth
import com.fresco.http.routes.{IngredientRoutes, RecipeRoutes}
import com.fresco.registries.{IngredientRegistry, RecipeRegistry}
import com.typesafe.config.{Config, ConfigFactory}

import scala.util.{Failure, Success}


//#main-class
object FrescoApp {
  //#start-http-server
  private def startHttpServer(port: Int, routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val cors = new CORSHandler {}
    val futureBinding = Http().newServerAt("localhost", port).bind(cors.corsHandler(routes))
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

      val awsConfig = config.getConfig(s"fresco.$environment.aws")
      val ingredientsTable = awsConfig.getConfig("storage").getString("ingredientsTableName")
      val recipesTable = awsConfig.getConfig("storage").getString("recipesTableName")
      val favouritesTable = awsConfig.getConfig("storage").getString("favouriteRecipesTableName")
      val bucketName = awsConfig.getConfig("storage").getString("bucketName")
      val s3Client = AWSClientsProvider.createS3PresignedUrlClient(awsConfig)
      val dynamoDBClient = AWSClientsProvider.createDynamoDBClient(awsConfig)

      val dynamoDBService = DynamoDBService(dynamoDBClient, ingredientsTable, recipesTable, favouritesTable)(context.executionContext)
      val s3Service = S3Service(s3Client, bucketName)(context.executionContext)

      val ingredientRepository = IngredientRepository(dynamoDBService, s3Service)(context.executionContext)
      val recipeRepository = RecipeRepository(dynamoDBService, s3Service)(context.executionContext)

      val ingredientRegistryActor = context.spawn(IngredientRegistry(ingredientRepository), "IngredientRegistryActor")
      context.watch(ingredientRegistryActor)
      val recipeRegistryActor = context.spawn(RecipeRegistry(recipeRepository), "RecipeRegistryActor")
      context.watch(recipeRegistryActor)

      val userPoolId = awsConfig.getConfig("auth").getString("userPoolId")
      val userPoolRegion = awsConfig.getConfig("auth").getString("userPoolRegion")
      val cognitoAuth = CognitoAuth(userPoolId, userPoolRegion)(context.executionContext)

      val allowedOrigin = config.getString("fresco.allowedOrigin")
      val port = config.getInt("fresco.port")
      val routes: Route = {
        concat(
          new IngredientRoutes(ingredientRegistryActor)(context.system).ingredientRoutes,
          new RecipeRoutes(cognitoAuth, recipeRegistryActor)(context.system).recipeRoutes
        )
      }
      startHttpServer(port, routes)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
    //#server-bootstrapping
  }
}
//#main-class
