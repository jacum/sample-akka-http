package com.queirozf.akkahttpseed.actors

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import scala.concurrent.Future

/**
  * Created by felipe on 12/06/16.
  */
class StockPriceConnector extends Actor with ActorLogging {

  import StockPriceConnector._
  import context.dispatcher

  implicit val system = context.system
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  def receive = {

    case GetQuote(ticker) =>
      getQuote(ticker) pipeTo sender()

  }

  private def getQuote(ticker:String): Future[Option[String]] = {
    ???
  }

}

object StockPriceConnector{

  case class GetQuote(ticker:String)

}
