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
import com.vtex.akkahttpseed.models.marshalling.CustomMarshallers
import CustomMarshallers._

import scala.concurrent.Future


/**
  * Companion object for the Actor
  *
  * props is an actor factory; it is located here to avoid serialization and race issues
  * since actor creation is async and with location transparent
  *
  * case objects / case classes are messages that this actor can handle
  *
  * This structure follows the Akka Recommended Practices for Actors
  * http://doc.akka.io/docs/akka/current/scala/actors.html#Recommended_Practices
  *
  */
object StockPriceConnector {

  def props(apiKey: String): Props = Props(new StockPriceConnector(apiKey))

  case class GetQuote(ticker: String, day: Int, month: Int, year: Int)

}

class StockPriceConnector(apiKey: String) extends Actor with ActorLogging {

  import StockPriceConnector._
  import context.dispatcher

  implicit val system = context.system
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  def receive = {

    case GetQuote(ticker, day, month, year) =>
      getSingleQuote(ticker, day, month, year) pipeTo sender()

  }

  private def getSingleQuote(ticker: String, day: Int, month: Int, year: Int): Future[Option[DailyQuoteResult]] = {

    val baseUri = s"https://www.quandl.com/api/v3/datasets/WIKI/$ticker.json"

    // Query creates query string params
    val query: Query = Query(
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

    val output: Future[Option[DailyQuoteResult]] = response.flatMap {
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
          // transform other result in a failure, no need to throw an exception
          case _ => {
            val externalError = Unmarshal(resp.entity).to[String].flatMap { body =>
              Future.failed(new ExternalResourceException(body))
            }
            externalError
          }
        }
    }
    output
  }
}

