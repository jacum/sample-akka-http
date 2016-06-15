package com.vtex.akkahttpseed.models.marshallers

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.vtex.akkahttpseed.models.{DailyQuoteResult, DatasetData}
import com.vtex.akkahttpseed.models.forms.GetQuoteModel
import spray.json.DefaultJsonProtocol

/**
  * Created by felipe.almeida@vtex.com.br on 14/06/16.
  */
object Implicits extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val getQuoteFormats = jsonFormat1(GetQuoteModel)
  implicit val datasetFormats = jsonFormat1(DatasetData)
  implicit val dailyQuoteFormats = jsonFormat1(DailyQuoteResult)

}
