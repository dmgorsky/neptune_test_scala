package com.example

//#price-registry-actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.mutable

final case class StatsCalculation(
    calculation_range: String,
    prices_sum: Float,
    prices_min: Float,
    prices_max: Float,
    prices_last: Float,
    prices_variance: Float
)

final case class AddBatch(
    symbol: Char,
    values: Array[Float]
)

final case class ClientGetStats(
    symbol: Char,
    k: Int
)

object PriceRegistry {
  // actor protocol
  sealed trait Command

  final case class GetStats(
      symbol: Char,
      k: Int,
      replyTo: ActorRef[GetStatsResponse]
  ) extends Command

  final case class GetStatsResponse(stats: Either[String, StatsCalculation])
      extends Command

  final case class PushPrices(
      symbol: Char,
      prices: Array[Float],
      replyTo: ActorRef[ActionPerformed]
  ) extends Command

  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(
    //initial state: empty price actors registry
    //map is mutable, so we don't change behavior with its updates
    scala.collection.mutable.Map[Char, ActorRef[PriceActor.Command]]()
  )

  private def registry(
      statsActorMap: mutable.Map[Char, ActorRef[PriceActor.Command]]
  ): Behavior[Command] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case PushPrices(symbol, prices, replyTo) =>
          //guard
          if (statsActorMap.size > 10) {
            replyTo ! ActionPerformed("Too many symbols")
            Behaviors.same
          } else {

            //get or spawn price actor
            val priceActor = statsActorMap.getOrElseUpdate(
              symbol,
              context.spawn(
                PriceActor(),
                symbol.toString
              )
            )
            priceActor ! PriceActor.PushPrices(prices, replyTo)
            Behaviors.same
          }
        case GetStats(symbol, k, replyTo) =>
          val priceActor = statsActorMap.get(symbol)
          priceActor match {
            case Some(priceActor) =>
              priceActor ! PriceActor.GetStats(k, replyTo)
            case None =>
              replyTo ! GetStatsResponse(Left(s"No such symbol $symbol"))
          }
          Behaviors.same
      }
    }
  }
}
