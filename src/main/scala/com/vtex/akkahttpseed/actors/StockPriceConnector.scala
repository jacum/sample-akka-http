package com.vtex.akkahttpseed.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.vtex.akkahttpseed.models.DailyQuoteResult
import com.vtex.akkahttpseed.models.marshallers.Implicits._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object StockPriceConnector {

  // actor factoring, safer to be in an companion object like this to not get in serialization and race issues
  // since actor creating is async and with location transparency
  def props(apiKey: String): Props = Props(new StockPriceConnector(apiKey))

  // actor supported messages
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

  private def getSingleQuote(ticker: String, day: Int, month: Int, year: Int): Future[Try[Option[DailyQuoteResult]]] = {

    val baseUri = s"https://www.quandl.com/api/v3/datasets/WIKI/$ticker.json"

    val query = Query(
      "order" -> "asc",
      "exclude_column_names" -> "true",
      "start_date" -> s"$year-$month-$day",
      "end_date" -> s"$year-$month-$day",
      "column_index" -> "4",
      "api_key" -> apiKey)

    val fullUri = Uri(baseUri).withQuery(query)

    log.warning(fullUri.toString())

    val req = HttpRequest(method = HttpMethods.GET, uri = fullUri)
    Http().singleRequest(req).flatMap { response =>
      response.status match {
        case StatusCodes.NotFound => Future(Success(None))
        case StatusCodes.OK => Unmarshal(response.entity).to[DailyQuoteResult].map { quote =>
          Success(Some(quote))
        }
        case _ => Unmarshal(response.entity).to[String].map { body =>
          Failure(new RuntimeException(body))
        }
      }
    }
  }
}

