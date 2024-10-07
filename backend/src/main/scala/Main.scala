import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.util.ByteString
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, GetItemRequest, PutItemRequest}
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.PutObjectRequest
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*
import java.io.{ByteArrayInputStream, File, FileOutputStream}
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import spray.json.*

import scala.concurrent.Future
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}

import scala.util.{Failure, Success, Try}

// Case classes for your data
case class Ingredient(id: String, name: String, imagePath: Option[String])
case class IngredientYield(id: String, amount: Option[Double], unit: Option[String])
case class Yield(yields: Int, ingredients: Seq[IngredientYield])
case class Step(index: Int, instructions: String, instructionsHTML: String)
case class Nutrition(name: String, amount: Double)
case class Label(text: String, handle: String)
case class Recipe(id: String, name: String, nutrition: Seq[Nutrition], yields: Seq[Yield], ingredients: Seq[Ingredient],
                  label: Option[Label], steps: Seq[Step], totalTime: String, websiteUrl: String, imagePath: Option[String],
                  cardLink: Option[String])
case class SearchResponse(total: Int, skip: Int, items: Seq[Recipe])

object RecipeJsonProtocol extends DefaultJsonProtocol {
  implicit val ingredientFormat: RootJsonFormat[Ingredient] = jsonFormat3(Ingredient.apply)
  implicit val ingredientYieldFormat: RootJsonFormat[IngredientYield] = jsonFormat3(IngredientYield.apply)
  implicit val yieldFormat: RootJsonFormat[Yield] = jsonFormat2(Yield.apply)
  implicit val stepFormat: RootJsonFormat[Step] = jsonFormat3(Step.apply)
  implicit val nutritionFormat: RootJsonFormat[Nutrition] = jsonFormat2(Nutrition.apply)
  implicit val labelFormat: RootJsonFormat[Label] = jsonFormat2(Label.apply)
  implicit val recipeFormat: RootJsonFormat[Recipe] = jsonFormat11(Recipe.apply)
  implicit val searchResponseFormat: RootJsonFormat[SearchResponse] = jsonFormat3(SearchResponse.apply)
}

import RecipeJsonProtocol._

object RecipeCrawler {
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = system.dispatcher

  val config: Config = ConfigFactory.load()
  val environment: String = config.getString("app.env")

  // AWS clients
  val awsConfig: Config = config.getConfig(s"app.$environment.aws")
  val accessKey = awsConfig.getString("accessKey")
  val secretKey = awsConfig.getString("secretKey")
  val credentials = new BasicAWSCredentials(accessKey, secretKey)
  val s3Client = AmazonS3ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(credentials))
    .build()
  val dynamoDBClient = AmazonDynamoDBClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(credentials))
    .build();

  val storageConfig: Config = config.getConfig(s"app.$environment.aws.storage")
  val recipesTableName: String = storageConfig.getString("recipesTableName")
  val ingredientsTableName: String = storageConfig.getString("ingredientsTableName")

  // S3 bucket name
  val s3BucketName = storageConfig.getString("bucketName")

  val helloFreshConfig: Config = config.getConfig(s"app.$environment.helloFresh")
  val siteUrl = helloFreshConfig.getString("siteUrl")
  val searchApiUrl = helloFreshConfig.getString("searchApiUrl")
  val imagesUrl = helloFreshConfig.getString("imagesUrl")

  var apiSearchParams = Map(
    "offset" -> "0",
    "limit" -> "100",
    "product" -> "classic-box|veggie-box|meal-plan|family-box",
    "locale" -> "nl-NL",
    "country" -> "nl",
    "max-prep-time" -> "60"
  )

  def fetchAndPersistRecipes(recipes: Seq[Recipe]): Future[Unit] = {
    Future.sequence(recipes.map { recipe =>
      // Conditional image upload (only if imagePath is present)
      val imageFuture: Future[Option[String]] = recipe.imagePath match {
        case Some(imagePath) => uploadImageToS3(recipe.id, imagePath).map(Some(_))
        case None => Future.successful(None) // No image to upload
      }

      // Conditional PDF upload (only if cardLink is present)
      val pdfFuture: Future[Option[String]] = recipe.cardLink match {
        case Some(cardLink) => uploadPdfToS3(cardLink, recipe.id).map(Some(_))
        case None => Future.successful(None) // No PDF to upload
      }

      // After both uploads (if applicable), store the recipe in DynamoDB
      for {
        imageUrlOpt <- imageFuture
        pdfUrlOpt <- pdfFuture
      } yield {
        storeIngredientsInDynamoDB(recipe.ingredients)

        // Store the recipe with the updated imagePath and cardLink (if applicable)
        storeRecipeInDynamoDB(
          recipe.copy(
            imagePath = imageUrlOpt.orElse(recipe.imagePath),
            cardLink = pdfUrlOpt.orElse(recipe.cardLink)
          )
        )
      }
    }).map(_ => ())
  }

  def getImageFormat(imagePath: String): Option[String] = {
    // Extract the file extension from the imagePath
    val extension = Try(imagePath.split('.').last.toLowerCase).toOption

    // Check if it's a valid image format (you can add more formats as needed)
    extension match {
      case Some(ext) if Seq("jpg", "jpeg", "png", "gif", "bmp", "tiff").contains(ext) => Some(ext)
      case _ => None
    }
  }

  // Upload image to S3 and remove after
  def uploadImageToS3(id: String, imagePath: String)(implicit ec: ExecutionContext, mat: Materializer): Future[String] = {
    val encodedImagePath = URLEncoder.encode(imagePath, StandardCharsets.UTF_8.toString)
    val imageUrl = s"$imagesUrl$encodedImagePath"
    val format = getImageFormat(imagePath).getOrElse("")

    val imageResponseFuture = Http().singleRequest(HttpRequest(uri = imageUrl))

    imageResponseFuture.flatMap {
      case HttpResponse(_, _, entity, _) =>
        val tempFile = File.createTempFile(s"image_$id", s".$format")
        val tempFilePath = tempFile.getAbsolutePath

        // Consume the entity and save to file
        val downloadFuture = entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map { imageBytes =>
          val fos = new FileOutputStream(tempFile)
          fos.write(imageBytes.toArray)
          fos.close()
          tempFile
        }

        // After file is downloaded, upload to S3 and remove the file
        downloadFuture.flatMap { file =>
          val s3Key = s"images/$id.$format"
          try {
            s3Client.putObject(new PutObjectRequest(s3BucketName, s3Key, file))
            Future.successful(s"s3://$s3BucketName/$s3Key")
          } finally {
            // Always remove the file
            Files.deleteIfExists(Paths.get(tempFilePath))
          }
        }

      case _ => Future.failed(new Exception("Failed to download image"))
    }
  }

  // Upload PDF to S3 and remove after
  def uploadPdfToS3(cardLink: String, recipeId: String)(implicit ec: ExecutionContext, mat: Materializer): Future[String] = {
    val pdfResponseFuture = Http().singleRequest(HttpRequest(uri = cardLink))

    pdfResponseFuture.flatMap {
      case HttpResponse(_, _, entity, _) =>
        val tempFile = File.createTempFile(s"pdf_$recipeId", ".pdf")
        val tempFilePath = tempFile.getAbsolutePath

        // Consume the entity and save to file
        val downloadFuture = entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map { pdfBytes =>
          val fos = new FileOutputStream(tempFile)
          fos.write(pdfBytes.toArray)
          fos.close()
          tempFile
        }

        // After file is downloaded, upload to S3 and remove the file
        downloadFuture.flatMap { file =>
          val s3Key = s"pdfs/$recipeId.pdf"
          try {
            s3Client.putObject(new PutObjectRequest(s3BucketName, s3Key, file))
            Future.successful(s"s3://$s3BucketName/$s3Key")
          } finally {
            // Always remove the file
            Files.deleteIfExists(Paths.get(tempFilePath))
          }
        }

      case _ => Future.failed(new Exception("Failed to download PDF"))
    }
  }

  def getNutritionItem(nutrition: Seq[Nutrition], name: String): String = {
    nutrition.find(_.name == name).map(_.amount.toString).getOrElse("0")
  }

  def getIngredientFromYield(yields: Seq[Yield], ingredientId: String): (String, String) = {
    yields.head.ingredients
      .find(_.id == ingredientId)
      .map(y => (y.amount.getOrElse(0).toString, y.unit.getOrElse("naar smaak")))
      .getOrElse(("0", "naar smaak"))
  }

  def storeIngredientsInDynamoDB(ingredients: Seq[Ingredient]): Unit = {
    ingredients.foreach { ingredient =>
      val getItemRequest = new GetItemRequest()
        .withTableName(ingredientsTableName)
        .withKey(Map("id" -> new AttributeValue().withS(ingredient.id)).asJava)

      val getItemResult = dynamoDBClient.getItem(getItemRequest)

      if (getItemResult.getItem == null) {
        val baseItem = Map(
          "id" -> new AttributeValue().withS(ingredient.id),
          "name" -> new AttributeValue().withS(ingredient.name)
        )

        val ingredientItemFuture = ingredient.imagePath match {
          case Some(imagePath) =>
            uploadImageToS3(ingredient.id, imagePath).map { uploadedImagePath =>
              (baseItem + ("imagePath" -> new AttributeValue().withS(uploadedImagePath))).asJava
            }
          case None =>
            Future.successful(baseItem.asJava) // No imagePath, return base map
        }

        ingredientItemFuture.map { ingredientMap =>
          val putItemRequest = new PutItemRequest()
            .withTableName(ingredientsTableName)
            .withItem(ingredientMap)

          dynamoDBClient.putItem(putItemRequest)
        }
      }
    }
  }

  def isRecipeStored(id: String): Boolean = {
    val getItemRequest = new GetItemRequest().withTableName(recipesTableName)
      .withKey(Map("id" -> new AttributeValue().withS(id)).asJava)
    dynamoDBClient.getItem(getItemRequest).getItem != null
  }

    // Store recipe in DynamoDB
  def storeRecipeInDynamoDB(recipe: Recipe): Unit = {
    // Convert ingredients into a list of maps
    val ingredientsList = recipe.ingredients.map { ingredient => {
      val ingredientYield = getIngredientFromYield(recipe.yields, ingredient.id)
      new AttributeValue().withM(Map(
        "id" -> new AttributeValue().withS(ingredient.id),
        "amount" -> new AttributeValue().withN(ingredientYield(0)),
        "unit" -> new AttributeValue().withS(ingredientYield(1))
      ).asJava)
    }}.asJava

    // Convert steps into a list of maps
    val stepsList = recipe.steps.map { step =>
      new AttributeValue().withM(Map(
        "index" -> new AttributeValue().withN(step.index.toString),
        "instructions" -> new AttributeValue().withS(step.instructions),
        "instructionsHTML" -> new AttributeValue().withS(step.instructionsHTML)
      ).asJava)
    }.asJava

    val recipeItem = Map(
      "id" -> new AttributeValue().withS(recipe.id),
      "name" -> new AttributeValue().withS(recipe.name),
      "totalTime" -> new AttributeValue().withS(recipe.totalTime),
      "websiteUrl" -> new AttributeValue().withS(recipe.websiteUrl),
      "imagePath" -> new AttributeValue().withS(recipe.imagePath.getOrElse("")),
      "pdfPath" -> new AttributeValue().withS(recipe.cardLink.getOrElse("")),
      "macros" -> new AttributeValue().withM(Map(
        "fats" -> new AttributeValue().withN(getNutritionItem(recipe.nutrition, "Vetten")),
        "carbs" -> new AttributeValue().withN(getNutritionItem(recipe.nutrition, "Koolhydraten")),
        "proteins" -> new AttributeValue().withN(getNutritionItem(recipe.nutrition, "Eiwitten"))
      ).asJava),
      "ingredientsPerPerson" -> new AttributeValue().withL(ingredientsList),
      "steps" -> new AttributeValue().withL(stepsList)
    ).asJava

    // Put the item into the DynamoDB table
    dynamoDBClient.putItem(recipesTableName, recipeItem)
  }

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
    val searchUrl = s"$searchApiUrl${constructSearchUrl()}"
    Http().singleRequest(HttpRequest(
      uri = searchUrl,
      headers = List(Authorization(OAuth2BearerToken(bearerToken)))
    )).flatMap {
      case HttpResponse(_, _, entity, _) =>
        Unmarshal(entity).to[String].map { jsonString =>
          // Parse the response JSON into Recipe objects
          jsonString.parseJson.convertTo[SearchResponse]
        }
      case _ => Future.failed(new Exception("Failed to retrieve search results"))
    }
  }

  def filterRecipes(recipes: Seq[Recipe]): Seq[Recipe] = {
    recipes
      .filter {recipe => recipe.label.forall(_.text != "Alleen opwarmen")}
      .filter {recipe => !isRecipeStored(recipe.id)}
  }

  // Main crawl function
  def crawl: Future[Unit] = {
    for {
      apiToken <- fetchApiToken()
      initialSearchResponse <- performSearch(apiToken)
      totalPages = (1000 - initialSearchResponse.skip) / apiSearchParams("limit").toInt
      _ <- (1 to totalPages).foldLeft(Future.successful(())) { (acc, pageNum) =>
        acc.flatMap { _ =>
          apiSearchParams += ("offset" -> (pageNum * apiSearchParams("limit").toInt).toString)
          println(s"Searching with offset ${apiSearchParams.get("offset")}...")
          performSearch(apiToken).flatMap { searchResponse =>
            val filteredRecipes = filterRecipes(searchResponse.items)
            fetchAndPersistRecipes(filteredRecipes).map { _ =>
              println(s"Downloaded and saved batch $pageNum of ${filteredRecipes.length} recipes")
            }
          }
        }
      }
    } yield {
      println("All batches completed successfully.")
    }
  }

  def main(args: Array[String]): Unit = {
    crawl.onComplete {
      case Success(_) => println("Crawl completed successfully")
      case Failure(e) => println(s"Failed: ${e.getMessage}")
    }
  }
}
