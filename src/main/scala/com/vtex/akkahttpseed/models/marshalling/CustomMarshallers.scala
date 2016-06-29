package com.vtex.akkahttpseed.models.marshalling

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.vtex.akkahttpseed.models.{DailyQuoteResult, DatasetData, GetQuoteModel, QueueMessage}
import spray.json.DefaultJsonProtocol

/***
  * Akka HTTP default Json Support is done using spray-json.
  *
  * http://doc.akka.io/docs/akka/current/scala/http/common/json-support.html
  * https://github.com/spray/spray-json
  */
object CustomMarshallers extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val getQuoteFormats = jsonFormat4(GetQuoteModel)
  implicit val datasetFormats = jsonFormat1(DatasetData)
  implicit val dailyQuoteFormats = jsonFormat1(DailyQuoteResult)
  implicit val queueMessageFormtas = jsonFormat2(QueueMessage)

}
