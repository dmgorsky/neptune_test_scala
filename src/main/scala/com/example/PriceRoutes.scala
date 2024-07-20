package com.example

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.example.JsonFormats._
import com.example.PriceRegistry._

import scala.concurrent.Future

//#import-json-formats
//#price-routes-class
class PriceRoutes(priceRegistry: ActorRef[PriceRegistry.Command])(implicit
    val system: ActorSystem[_]
) {

  //#price-routes-class
  //#import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(
    system.settings.config.getDuration("my-app.routes.ask-timeout")
  )

  def pushPrices(symbol: Char, prices: Array[Float]): Future[ActionPerformed] =
    priceRegistry.ask(PriceRegistry.PushPrices(symbol, prices, _))

  def getStats(symbol: Char, k: Int): Future[GetStatsResponse] =
    priceRegistry.ask(PriceRegistry.GetStats(symbol, k, _))

  val priceRoutes: Route =
    concat(
      pathPrefix("stats") {
        parameters(Symbol("symbol").as[String], Symbol("k").as[String]) { (symbols, k) =>
          get {
            val futureResponse = getStats(symbols.head, k.toInt)
            onSuccess(futureResponse) { result =>
              complete(StatusCodes.OK, result)
            }
          }
        }
      },
      pathPrefix("add_batch") {
        post {
          entity(as[AddBatch]) { prices =>
            val futureResponse = pushPrices(prices.symbol, prices.values)
            onSuccess(futureResponse) { result =>
              complete(StatusCodes.OK, result)
            }
          }
        }
      }
    )
}
