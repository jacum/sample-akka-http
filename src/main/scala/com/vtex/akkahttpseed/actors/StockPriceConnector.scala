package com.vtex.akkahttpseed.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.vtex.akkahttpseed.models.DailyQuoteResult
import com.vtex.akkahttpseed.models.errors.ExternalResourceException
import com.vtex.akkahttpseed.models.marshallers.Implicits._

import scala.concurrent.Future

object StockPriceConnector {

  // actor "factory" - it's safer to do this in a companion object like this
  // so as to avoid serialization issues and race conditions, since
  // actor creation is asynchronous and location transparent
  // see also: http://doc.akka.io/docs/akka/current/scala/actors.html#props
  def props(apiKey: String): Props = Props(new StockPriceConnector(apiKey))

  // messages this actor supports:

  case class GetQuote(ticker: String, day: Int, month: Int, year: Int)

}

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
      getSingleQuote(ticker, day, month, year) pipeTo sender()

  }

  private def getSingleQuote(ticker: String, day: Int, month: Int, year: Int): Future[Option[DailyQuoteResult]] = {

    val baseUri = s"https://www.quandl.com/api/v3/datasets/WIKI/$ticker.json"

    val query = Query(
      "order" -> "asc",
      "exclude_column_names" -> "true",
      "start_date" -> s"$year-$month-$day",
      "end_date" -> s"$year-$month-$day",
      "column_index" -> "4",
      "api_key" -> apiKey)


    val fullUri = Uri(baseUri).withQuery(query)
    log.info("calling {}", fullUri.toString())
    val req = HttpRequest(method = HttpMethods.GET, uri = fullUri)
    val response = Http().singleRequest(req)
    val output = response.flatMap {
      case resp =>
        resp.status match {
          // format result
          case StatusCodes.OK => {
            val dailyQuote = Unmarshal(resp.entity).to[DailyQuoteResult]
            val someQuote = dailyQuote.map { quote => Some(quote) }
            someQuote
          }
          case StatusCodes.NotFound =>
            Future.successful(None)
          // transform result in a failure, no need to throw an exception
          case _ => {
            val externalError = Unmarshal(resp.entity).to[String].flatMap { body =>
              // transform result in a custom failure, no need to throw an exception
              Future.failed(new ExternalResourceException(body))
            }
            externalError
          }
        }
    }
    output
  }
}

