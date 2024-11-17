package com.fresco.http.auth

import akka.actor.ActorSystem
import org.slf4j.LoggerFactory
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import spray.json.*
import spray.json.DefaultJsonProtocol.StringJsonFormat

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import java.math.BigInteger
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import scala.concurrent.duration.*

// Class to handle Cognito authentication
class CognitoAuth(userPoolId: String, region: String)(implicit ec: ExecutionContext) {

  // Create a logger instance
  private val log = LoggerFactory.getLogger(classOf[CognitoAuth])

  // Cache for public keys
  private var cachedPublicKeys: Option[Map[String, (String, String)]] = None
  private var keysLastFetched: Long = 0
  private val cacheDuration: FiniteDuration = 1.hour // Set your desired cache duration

  // Function to fetch public keys from Cognito
  def fetchCognitoPublicKeys()(implicit system: ActorSystem): Future[Map[String, (String, String)]] = {
    val jwksUrl = s"https://cognito-idp.$region.amazonaws.com/$userPoolId/.well-known/jwks.json"
    log.info(s"Fetching public keys from: $jwksUrl")

    for {
      response <- Http().singleRequest(HttpRequest(uri = jwksUrl))
      json <- Unmarshal(response.entity).to[String]
    } yield {
      // Parse JSON and handle potential errors
      val keysField = json.parseJson.asJsObject.fields.get("keys")

      keysField match {
        case Some(JsArray(keys)) =>
          log.info(s"Successfully fetched ${keys.size} keys from Cognito.")
          // Convert each JsObject to a tuple and collect them into a Map
          keys.map { key =>
            val keyObj = key.asJsObject
            val kid = keyObj.fields("kid").convertTo[String] // "kid" field in JSON
            val n = keyObj.fields("n").convertTo[String]     // "n" field in JSON
            val e = keyObj.fields("e").convertTo[String]     // "e" field in JSON
            kid -> (n, e) // Create a tuple (kid, (n, e)) for the key
          }.toMap // Convert to Map
        case _ =>
          log.error("Invalid JSON format: 'keys' field is missing or not an array")
          throw new Exception("Invalid JSON format: 'keys' field is missing or not an array")
      }
    }
  }

  // Function to get public keys, using cache
  def getPublicKeys()(implicit system: ActorSystem): Future[Map[String, (String, String)]] = {
    val now = System.currentTimeMillis()
    if (cachedPublicKeys.isEmpty || (now - keysLastFetched) > cacheDuration.toMillis) {
      log.info("Public keys cache is empty or expired. Fetching new keys.")
      // Fetch new keys and update cache
      fetchCognitoPublicKeys().map { keys =>
        cachedPublicKeys = Some(keys)
        keysLastFetched = now
        keys
      }
    } else {
      log.info("Returning cached public keys.")
      // Return cached keys
      Future.successful(cachedPublicKeys.get)
    }
  }

  // Function to validate the token and extract the user ID
  def validateToken(token: String)(implicit system: ActorSystem): Future[Option[String]] = {
    log.info("Validating token.")

    for {
      publicKeys <- getPublicKeys()
      decodedJWT <- Future {
        Try {
          val jwtRegex = "^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$"
          if (!token.matches(jwtRegex)) {
            log.error("Token is not properly formatted.")
            throw new Exception("Malformed token")
          }

          val jwt = JWT.decode(token)
          val kid = jwt.getKeyId
          log.info(s"Token decoded. Key ID: $kid")

          val (n, e) = publicKeys.get(kid).getOrElse {
            log.error(s"Invalid key ID: $kid")
            throw new Exception("Invalid key ID")
          }

          val keyFactory = KeyFactory.getInstance("RSA")
          val publicKey: RSAPublicKey = keyFactory.generatePublic(
            new RSAPublicKeySpec(
              new BigInteger(1, Base64.getUrlDecoder.decode(n)),
              new BigInteger(1, Base64.getUrlDecoder.decode(e))
            )
          ).asInstanceOf[RSAPublicKey]

          val algorithm = Algorithm.RSA256(publicKey)
          val verifier = JWT.require(algorithm).build()
          verifier.verify(token)
        }
      }
    } yield {
      decodedJWT match {
        case Success(decoded) =>
          log.info("Token successfully validated.")
          Some(decoded.getClaim("sub").asString()) // Extract user ID from the token
        case Failure(ex) =>
          log.error(s"Token validation failed: ${ex.getMessage}")
          None // Token is invalid
      }
    }
  }

}