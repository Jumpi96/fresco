import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import com.fresco.domain.models.Ingredient
import com.fresco.domain.repositories.IngredientRepository
import com.fresco.domain.services.{DynamoDBService, S3Service}
import com.fresco.registries.IngredientRegistry
import com.fresco.registries.IngredientRegistry.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

// Mock the DynamoDBService for the tests
class MockDynamoDBService(implicit ec: ExecutionContext) extends DynamoDBService(null, "mockTable", "mockTableTwo") {
  override def getIngredients(lastEvaluatedId: Option[String], limit: Int): Future[(Seq[Ingredient], Option[String])] = {
    // Return a mocked list of ingredients and None for lastEvaluatedId
    Future.successful((Seq(Ingredient("1", "Ingredient1", None), Ingredient("2", "Ingredient2", Some("imagePath"))), None))
  }

  override def getIngredient(id: String): Future[Option[Ingredient]] = {
    // Mock getting an ingredient based on ID
    id match {
      case "1" => Future.successful(Some(Ingredient("1", "Ingredient1", None)))
      case _ => Future.successful(None)
    }
  }
}

// Mock the S3Service for the tests
class MockS3Service(implicit ec: ExecutionContext) extends S3Service(null, "bucket") {
  override def generatePresignedUrl(s3Path: String): Future[URL] = Future {
    URL("http://example.com")
  }
}

class IngredientRegistrySpec extends AnyWordSpec with Matchers with ScalaFutures {

  val testKit = ActorTestKit()
  implicit val ec: ExecutionContext = testKit.system.executionContext

  // Create a TestProbe to simulate the replyTo ActorRef
  val probe: TestProbe[GetIngredientsResponse] = testKit.createTestProbe[GetIngredientsResponse]()
  val ingredientRepository = new IngredientRepository(new MockDynamoDBService, new MockS3Service)
  val ingredientRegistry: ActorRef[Command] = testKit.spawn(IngredientRegistry(ingredientRepository))

  "IngredientRegistry" should {

    "fetch ingredients successfully" in {
      // Send a GetIngredients message to the actor
      ingredientRegistry ! GetIngredients(50, None, probe.ref)

      // Expect the mocked ingredients in the reply
      val response = probe.receiveMessage()
      response.ingredients should have size 2
      response.lastEvaluatedId shouldBe None
      response.ingredients.head.name shouldBe "Ingredient1"
    }

    "return an ingredient by ID if it exists" in {
      val probe = testKit.createTestProbe[GetIngredientResponse]()
      ingredientRegistry ! GetIngredient("1", probe.ref)

      // Expect the ingredient in the response
      val response = probe.receiveMessage()
      response.maybeIngredient shouldBe defined
      response.maybeIngredient.get.name shouldBe "Ingredient1"
    }

    "return None if the ingredient does not exist" in {
      val probe = testKit.createTestProbe[GetIngredientResponse]()
      ingredientRegistry ! GetIngredient("non-existent-id", probe.ref)

      // Expect None in the response
      val response = probe.receiveMessage()
      response.maybeIngredient should not be defined
    }
  }

  def afterAll(): Unit = {
    testKit.shutdownTestKit()
  }
}
