package com.example

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.example.PriceRegistry.{ActionPerformed, GetStatsResponse}

import scala.collection.mutable

object PriceActor {
  sealed trait Command

  final case class GetStats(k: Int, replyTo: ActorRef[GetStatsResponse])
      extends Command

  final case class PushPrices(
      prices: Array[Float],
      replyTo: ActorRef[ActionPerformed]
  ) extends Command

  def apply(): Behavior[Command] = priceActorBehavior(
    new mutable.ArrayDeque[Float](100_000_000),
    0
  )

  private def getStats(
      prices: mutable.ArrayDeque[Float],
      endIndex: Int,
      k: Int
  ): StatsCalculation = {
    var sSum = 0f
    var sMin = Float.MaxValue
    var sMax = Float.MinValue
    var sCount = 0
    val sLast = prices(endIndex - 1)
    var startFrom = endIndex - k - 2
    if (startFrom < 0) {
      startFrom = 0
    }
    var it = new PricesIterator(endIndex, k)
    for (i <- it) {
      val price = prices(i - 1)
      sSum += price
      sMin = math.min(sMin, price)
      sMax = math.max(sMax, price)
      sCount += 1
    }
    val mean = sSum / sCount

    var sVariance = 0f
    it = new PricesIterator(endIndex, k) //2nd pass
    for (i <- it) {
      val price = prices(i - 1)
      sVariance += (price - mean) * (price - mean)
    }
    sVariance = sVariance / sCount

    val result = StatsCalculation(
      s"$startFrom..${startFrom + sCount - 1}",
      sSum,
      sMin,
      sMax,
      sLast,
      sVariance
    )
    result
  }

  private def priceActorBehavior(
      prices_history: mutable.ArrayDeque[Float],
      currSize: Int
  ): Behaviors.Receive[Command] = {
    Behaviors.receiveMessage[Command] {
      case PushPrices(prices, replyTo) =>
        prices_history.addAll(prices)
        replyTo ! ActionPerformed("Prices pushed")
        priceActorBehavior(
          prices_history,
          math.min(currSize + prices.length, 100_000_000)
        )
      case GetStats(k, replyTo) =>
        replyTo ! GetStatsResponse(
          Right(
            getStats(prices_history, currSize, k)
          )
        )
        Behaviors.same
    }
  }

}

class PricesIterator(endIndex: Int, k: Int) extends Iterable[Int] {
  private val maxK: Int = 100_000_000
  private val realK: Int =
    math.min(math.min(math.pow(10, k).toInt, maxK), endIndex)
  private var currInd = endIndex
  private var remainder = realK
  def iterator: Iterator[Int] = new Iterator[Int] {
    def hasNext: Boolean = remainder > 0

    def next(): Int = {
      remainder = remainder - 1
      if (currInd < 0) {
        currInd = realK - 1
      }
      currInd = currInd - 1
      currInd + 1
    }
  }
}
