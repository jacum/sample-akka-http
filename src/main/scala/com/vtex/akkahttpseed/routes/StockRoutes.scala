package com.vtex.akkahttpseed.routes

import akka.actor.{ActorSystem, ActorRef}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.ask
import com.vtex.akkahttpseed.actors.StockPriceConnector
import com.vtex.akkahttpseed.models.DailyQuote

/**
  * Created by felipe on 14/06/16.
  */
class StockRoutes(stockPriceConnector: ActorRef)(implicit system: ActorSystem) {

  implicit val ec = system.dispatcher

  def routes = {
    path("stocks" / Segment) { ticker =>
      getRandomDailyQuote(ticker)
    }
  }
  private def getRandomDailyQuote(ticker: String) = {
    val tickerUpper = ticker.toUpperCase
    complete {
      (stockPriceConnector ? StockPriceConnector.GetQuote(tickerUpper, 1, 1, 2015)).mapTo[Option[DailyQuote]].map {
        case Some(quote) => HttpResponse(entity = HttpEntity(quote.toString))
        case None => HttpResponse(StatusCodes.NotFound)
      }
    }
  }

}
