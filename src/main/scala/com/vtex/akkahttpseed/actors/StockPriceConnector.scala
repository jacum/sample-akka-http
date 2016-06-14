package com.vtex.akkahttpseed.actors

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.http.scaladsl.Http

import scala.concurrent.Future

/**
  * Created by felipe on 12/06/16.
  */
class StockPriceConnector(apiKey: String) extends Actor with ActorLogging {

  import StockPriceConnector._
  import context.dispatcher

  /**
    * https://www.quandl.com/docs/api#complex-data-request
    */

  implicit val system = context.system
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  def receive = {

    case GetQuote(ticker, day, month, year) =>
      getQuote(ticker, day, month, year) pipeTo sender()

  }

  private def getQuote(ticker: String, day: Int, month: Int, year: Int): Future[Option[String]] = {

    val baseUri = "https://www.quandl.com/api/v3/datasets/WIKI/"

    val query = Query(
      "order" -> "asc",
      "exclude_column_names" -> "true",
      "start_date" -> s"$year-$month-$day",
      "end_date" -> s"$year-$month-$day",
      "column_index" -> "4",
      "api_key" -> apiKey)


    val fullUri = Uri()
    val req = HttpRequest(method = HttpMethods.GET, uri = Uri(fullUri), entity = HttpEntity(ContentTypes.`application/json`, searchQuery))
    Http().singleRequest(req)
  }

}

object StockPriceConnector {

  case class GetQuote(ticker: String, day: Int, month: Int, year: Int)

}
