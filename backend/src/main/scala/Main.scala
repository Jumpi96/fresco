import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import java.io.File
import java.nio.file.{Files, Paths}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._
import DefaultJsonProtocol._
import scala.util.{Failure, Success}

// Models
case class RecipeCard(name: String, cardLink: String)
case class SearchResponse(total: Int, skip: Int, items: Seq[RecipeCard])

// JSON deserialization with Spray Json
object RecipeJsonProtocol extends DefaultJsonProtocol {
  implicit val recipeCardFormat: RootJsonFormat[RecipeCard] = jsonFormat2(RecipeCard) // Explicit return type
  implicit val searchResponseFormat: RootJsonFormat[SearchResponse] = jsonFormat3(SearchResponse)
}

import RecipeJsonProtocol._

object RecipeCrawler {
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = system.dispatcher

  val siteUrl = "https://www.hellofresh.com"
  val apiUrl = "https://gw.hellofresh.com/api/"
  val searchEndpoint = "recipes/search?"
  var recipeCardSaveDirectory = "./recipe-card-pdfs"

  var apiSearchParams = Map(
    "offset" -> "0",
    "limit" -> "250",
    "product" -> "classic-box|veggie-box|meal-plan|family-box",
    "locale" -> "en-US",
    "country" -> "us",
    "max-prep-time" -> "60"
  )

  // Fetch the API token by scraping the website
  def fetchApiToken(): Future[String] = {
    Http().singleRequest(HttpRequest(uri = siteUrl)).flatMap { response =>
      Unmarshal(response.entity).to[String].map { body =>
        val tokenRegex = """"access_token":"([^"]+)"""".r
        tokenRegex.findFirstMatchIn(body).map(_.group(1)) match {
          case Some(token) => token
          case None        => throw new Exception("Access token not found")
        }
      }
    }
  }

  // Construct search URL
  def constructSearchUrl(): String = {
    apiSearchParams.map { case (key, value) => s"$key=$value" }.mkString("&")
  }

  // Perform search with the API token
  def performSearch(bearerToken: String): Future[SearchResponse] = {
    val searchUrl = s"$apiUrl$searchEndpoint${constructSearchUrl()}"
    Http().singleRequest(HttpRequest(
      uri = searchUrl,
      headers = List(headers.Authorization(headers.OAuth2BearerToken(bearerToken)))
    )).flatMap { response =>
      Unmarshal(response.entity).to[SearchResponse]
    }
  }

  // Download recipe cards with retry mechanism
  def downloadRecipeCards(items: Seq[RecipeCard], maxRetryAttempts: Int = 3): Future[Unit] = {
    val downloadFutures = items.map { item =>
      def retryDownload(attempt: Int): Future[Unit] = {
        if (attempt >= maxRetryAttempts) {
          Future.failed(new Exception(s"Failed to download ${item.name} after $maxRetryAttempts attempts"))
        } else {
          Http().singleRequest(HttpRequest(uri = item.cardLink)).flatMap { response =>
            response.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map { data =>
              val path = Paths.get(s"$recipeCardSaveDirectory/${item.name}.pdf")
              if (!Files.exists(path)) {
                Files.write(path, data.toArray)
              }
            }
          }.recoverWith { case _ =>
            akka.pattern.after(2.seconds, using = system.scheduler)(retryDownload(attempt + 1))
          }
        }
      }
      retryDownload(0)
    }
    Future.sequence(downloadFutures).map(_ => ())
  }

  // Main crawl function
  def crawl(settings: Map[String, String]): Future[Unit] = {
    if (settings.contains("locale")) {
      apiSearchParams += ("locale" -> s"${settings("locale").toLowerCase}")
      apiSearchParams += ("country" -> settings("locale").toLowerCase)
    }

    settings.get("recipeCardSaveDirectory").foreach { dir =>
      recipeCardSaveDirectory = dir
      new File(recipeCardSaveDirectory).mkdirs()
    }

    for {
      apiToken <- fetchApiToken()
      initialSearchResponse <- performSearch(apiToken)
      totalPages = (initialSearchResponse.total - initialSearchResponse.skip) / apiSearchParams("limit").toInt
      _ <- (1 to totalPages).foldLeft(Future.successful(())) { (acc, pageNum) =>
        acc.flatMap { _ =>
          apiSearchParams += ("offset" -> (pageNum * apiSearchParams("limit").toInt).toString)
          performSearch(apiToken).flatMap { searchResponse =>
            downloadRecipeCards(searchResponse.items).map { _ =>
              println(s"Downloaded and saved batch $pageNum of ${searchResponse.items.length} recipes")
            }
          }
        }
      }
    } yield {
      println("All batches completed successfully.")
    }
  }

  def main(args: Array[String]): Unit = {
    val settings = Map("locale" -> "US", "recipeCardSaveDirectory" -> "./recipe-cards")
    crawl(settings).onComplete {
      case Success(_) => println("Crawl completed successfully")
      case Failure(e) => println(s"Failed: ${e.getMessage}")
    }
  }
}
