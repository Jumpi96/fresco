package com.fresco.http.routes

import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.fresco.http.formats.JsonFormats
import com.fresco.registries.RecipeRegistry
import com.fresco.registries.RecipeRegistry.{AddFavouriteRequest, AddFavouriteResponse, GetRecipe, GetRecipeResponse, GetRecipesResponse, RemoveFavouriteResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.concurrent.ExecutionContext.Implicits.global
import java.math.BigInteger
import java.security.spec.RSAPublicKeySpec
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.collection.mutable
import akka.actor.Scheduler
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey

//#import-json-formats
//#recipe-routes-class
class RecipeRoutes(recipeRegistry: ActorRef[RecipeRegistry.Command])(implicit val system: ActorSystem[_]) {

  //#recipe-routes-class
  import JsonFormats.*
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
  //#import-json-formats

  implicit val ec: ExecutionContext = system.executionContext

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("fresco.routes.ask-timeout"))

  //#all-routes
  //#recipes-get
  //#recipes-get
  val allRecipeRoutes: Route =
    pathPrefix("api" / "recipes") {
      extractRequestContext { ctx =>
        val tokenOpt = ctx.request.headers.find(_.name() == "Authorization").map(_.value().stripPrefix("Bearer "))
        tokenOpt match {
          case Some(token) =>
            onComplete(validateToken(token)) {
              case Success(Some(user)) => // User is authenticated
                concat(
                  pathEnd {
                    parameters("pageSize".as[Int].optional, "lastEvaluatedId".optional) { (pageSize, lastEvaluatedId) =>
                      val size = pageSize.getOrElse(50)
                      val futureRecipes: Future[RecipeRegistry.GetRecipesResponse] =
                        recipeRegistry.ask(replyTo => RecipeRegistry.GetRecipes(size, lastEvaluatedId, replyTo))

                      onComplete(futureRecipes) {
                        case Success(GetRecipesResponse(recipes, lastEvaluatedId)) =>
                          complete(GetRecipesResponse(recipes, lastEvaluatedId))
                        case Failure(ex) =>
                          complete(StatusCodes.InternalServerError -> s"Failed to fetch recipes: ${ex.getMessage}")
                      }
                    }
                  },
                  path(Segment) { id =>
                    get {
                      parameters("userId".as[String]) { (userId) =>
                        onComplete(recipeRegistry.ask(replyTo => GetRecipe(id, userId, replyTo))) {
                          case Success(GetRecipeResponse(recipe, isFavourite)) =>
                            complete(GetRecipeResponse(recipe, isFavourite))
                          case Success(_) =>
                            complete(StatusCodes.NotFound -> s"Recipe with ID $id not found")
                          case Failure(exception) =>
                            complete(StatusCodes.InternalServerError -> s"Failed to fetch recipe: ${exception.getMessage}")
                        }
                      }
                    }
                  })
              case Success(None) =>
                complete(StatusCodes.Unauthorized -> "Invalid token")
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError -> s"Token validation failed: ${ex.getMessage}")
            }
          case None =>
            complete(StatusCodes.Unauthorized -> "Missing token")
        }
      }
    }

  val favouriteRoutes: Route =
    pathPrefix("api" / "favourites") {
      concat(
        // GET method for fetching favourite recipes
        get {
          parameters("userId".as[String], "pageSize".as[Int].optional, "lastEvaluatedId".optional) { (userId, pageSize, lastEvaluatedId) =>
            val size = pageSize.getOrElse(50)
            val futureRecipes: Future[RecipeRegistry.GetRecipesResponse] =
              recipeRegistry.ask(replyTo => RecipeRegistry.GetFavouriteRecipes(userId, size, lastEvaluatedId, replyTo))

            onComplete(futureRecipes) {
              case Success(GetRecipesResponse(recipes, lastEvaluatedId)) =>
                complete(GetRecipesResponse(recipes, lastEvaluatedId))
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError -> s"Failed to fetch favourite recipes: ${ex.getMessage}")
            }
          }
        },
        post {
          entity(as[AddFavouriteRequest]) { request =>
            val futureResponse: Future[RecipeRegistry.AddFavouriteResponse] =
              recipeRegistry.ask(replyTo => RecipeRegistry.AddFavouriteRecipe(request.userId, request.recipeId, replyTo))

            onComplete(futureResponse) {
              case Success(AddFavouriteResponse(true)) =>
                complete(StatusCodes.OK -> "Recipe added to favourites.")
              case Success(AddFavouriteResponse(false)) =>
                complete(StatusCodes.BadRequest -> "Failed to add recipe to favourites.")
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError -> s"Failed to add favourite recipe: ${ex.getMessage}")
            }
          }
        },
        delete {
          parameters("userId".as[String], "recipeId".as[String]) { (userId, recipeId) =>
            val futureResponse: Future[RecipeRegistry.RemoveFavouriteResponse] =
              recipeRegistry.ask(replyTo => RecipeRegistry.RemoveFavouriteRecipe(userId, recipeId, replyTo))

            onComplete(futureResponse) {
              case Success(RemoveFavouriteResponse(success)) =>
                complete(StatusCodes.OK -> "Recipe removed from favourites.")
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError -> s"Failed to remove favourite recipe: ${ex.getMessage}")
            }
          }
        }
      )
    }

  val recipeRoutes = concat(allRecipeRoutes, favouriteRoutes)
  //#all-routes

  // Cache for public keys
  private var cachedPublicKeys: Option[Map[String, (String, String)]] = None
  private var keysLastFetched: Long = 0
  private val cacheDuration: FiniteDuration = 1.hour // Set your desired cache duration

  // Function to fetch public keys from Cognito
  def fetchCognitoPublicKeys(userPoolId: String, region: String)(implicit ec: ExecutionContext): Future[Map[String, (String, String)]] = {
    val jwksUrl = s"https://cognito-idp.$region.amazonaws.com/$userPoolId/.well-known/jwks.json"
    
    for {
      response <- Http().singleRequest(HttpRequest(uri = jwksUrl))
      json <- Unmarshal(response.entity).to[String]
    } yield {
      // Parse JSON and handle potential errors
      val keysField = json.parseJson.asJsObject.fields.get("keys")
      
      keysField match {
        case Some(JsArray(keys)) =>
          // Convert each JsObject to a tuple and collect them into a Map
          keys.map { key =>
            val keyObj = key.asJsObject
            val kid = keyObj.fields("kid").convertTo[String] // "kid" field in JSON
            val n = keyObj.fields("n").convertTo[String]     // "n" field in JSON
            val e = keyObj.fields("e").convertTo[String]     // "e" field in JSON
            kid -> (n, e) // Create a tuple (kid, (n, e)) for the key
          }.toMap // Convert to Map
        case _ =>
          throw new Exception("Invalid JSON format: 'keys' field is missing or not an array")
      }
    }
  }

  // Function to get public keys, using cache
  def getPublicKeys(userPoolId: String, region: String): Future[Map[String, (String, String)]] = {
    val now = System.currentTimeMillis()
    if (cachedPublicKeys.isEmpty || (now - keysLastFetched) > cacheDuration.toMillis) {
      // Fetch new keys and update cache
      fetchCognitoPublicKeys(userPoolId, region).map { keys =>
        cachedPublicKeys = Some(keys)
        keysLastFetched = now
        keys
      }
    } else {
      // Return cached keys
      Future.successful(cachedPublicKeys.get)
    }
  }

  // Function to validate the token and extract the user ID
  def validateToken(token: String): Future[Option[String]] = {
    val userPoolId = "your_user_pool_id" // Replace with your User Pool ID
    val region = "your_region" // Replace with your AWS region

    for {
      publicKeys <- getPublicKeys(userPoolId, region)
      decodedJWT <- Future {
        Try {
          val jwt = JWT.decode(token)
          val kid = jwt.getKeyId
          val (n, e) = publicKeys.get(kid).getOrElse(throw new Exception("Invalid key ID"))
          val algorithm = {
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey: RSAPublicKey = keyFactory.generatePublic(new RSAPublicKeySpec(new BigInteger(n, 16), new BigInteger(e, 16))).asInstanceOf[RSAPublicKey]
            Algorithm.RSA256(publicKey)
          }
          val verifier = JWT.require(algorithm).build()
          verifier.verify(token)
        }
      }
    } yield {
      decodedJWT match {
        case Success(decoded) =>
          Some(decoded.getClaim("sub").asString()) // Extract user ID from the token
        case Failure(_) =>
          None // Token is invalid
      }
    }
  }
}
