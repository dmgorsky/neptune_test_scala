package com.example

import com.example.PriceRegistry.{ActionPerformed, GetStatsResponse}

//#json-formats
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val ackFormat: RootJsonFormat[ActionPerformed] = jsonFormat1(
    ActionPerformed.apply
  )
  implicit val getStatsCalculation: RootJsonFormat[StatsCalculation] =
    jsonFormat6(
      StatsCalculation.apply
    )
  implicit val respFormat: RootJsonFormat[GetStatsResponse] = jsonFormat1(
    GetStatsResponse.apply
  )
  implicit val addBatchFormat: RootJsonFormat[AddBatch] = jsonFormat2(
    AddBatch.apply
  )

}
//#json-formats
