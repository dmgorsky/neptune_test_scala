package com.example

//#price-routes-spec
//#test-top
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ContentTypes, MessageEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.example.JsonFormats._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

//#set-up
class PriceRoutesSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest {
  //#test-top

  // the Akka HTTP route testkit does not yet support a typed actor system (https://github.com/akka/akka-http/issues/2036)
  // so we have to adapt for now
  lazy val testKit: ActorTestKit = ActorTestKit()
  implicit def typedSystem: ActorSystem[_] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  // Here we need to implement all the abstract members of PriceRoutes.
  // We use the real PriceRegistryActor to test it while we hit the Routes,
  // but we could "mock" it by implementing it in-place or by using a TestProbe
  // created with testKit.createTestProbe()
  val priceRegistry = testKit.spawn(PriceRegistry())
  lazy val routes = new PriceRoutes(priceRegistry).priceRoutes

  // use the json formats to marshal and unmarshall objects in the test
  //#set-up

  //#actual-test
  "PriceRoutes" should {

    "return no prices if no ask request" in {
      Get("/stats") ~> routes ~> check {
        handled shouldBe false
      }
    }

    "return zero prices" in {
      Get("/stats/?symbol=q&k=4") ~> routes ~> check {
        handled shouldBe true
        status should ===(StatusCodes.OK)
      }
    }

    "post prices" in {
      val pricesBatch =
        AddBatch('w', Array[Double](1.1, 2.2, 3.3, 4.4, 5.5, 6.6))
      val pricesEntity =
        Marshal(pricesBatch)
          .to[MessageEntity]
          .futureValue // futureValue is from ScalaFutures
      val request = Post("/add_batch/").withEntity(pricesEntity)
      request ~> routes ~> check {
        handled shouldBe true
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"description":"Prices pushed"}""")
      }
    }

  }
  //#actual-test

  //#set-up
}
//#set-up
//#price-routes-spec
