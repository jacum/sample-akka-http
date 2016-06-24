package com.vtex.akkahttpseed.models.marshallers

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.vtex.akkahttpseed.models.{DailyQuoteResult, DatasetData, GetQuoteModel, QueueMessage}
import spray.json.DefaultJsonProtocol

object CustomMarshallers extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val getQuoteFormats = jsonFormat4(GetQuoteModel)
  implicit val datasetFormats = jsonFormat1(DatasetData)
  implicit val dailyQuoteFormats = jsonFormat1(DailyQuoteResult)
  implicit val queueMessageFormtas = jsonFormat2(QueueMessage)

}
