package com.vtex.akkahttpseed

import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.actor.{ActorSystem, Props}
import com.vtex.akkahttpseed.actors.{QueueConnector, StockPriceConnector}
import com.vtex.akkahttpseed.routes.QueueRoutes
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory

/**
  * Created by felipe on 12/06/16.
  */
object AkkaHttpScalaDockerSeed extends App {


  implicit val system = ActorSystem("main-actor-system")
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system))
  implicit val ec = system.dispatcher

  val conf = ConfigFactory.load()
  val queueName = conf.getString("custom.queue.name")
  val apiKey = conf.getString("custom.stocks.api-key")

  // actors
  val queueConnector = system.actorOf(Props(classOf[QueueConnector],queueName), "queue-connector")
  val stockPriceConnector = system.actorOf(Props(classOf[StockPriceConnector], apiKey), "stock-price-connector")

  // route definitions
  val queueRoutes = new QueueRoutes(queueConnector,stockPriceConnector)

  // merge all routes here
  def allRoutes = {
    queueRoutes.routes
  }

  Http().bindAndHandle(allRoutes, "0.0.0.0", 5000)
}
