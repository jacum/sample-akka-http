package com.vtex.akkahttpseed

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.config.ConfigFactory
import com.vtex.akkahttpseed.actors.{MessageWorker, QueueConnector, StockPriceConnector}
import com.vtex.akkahttpseed.routes.{MonitoringRoutes, QueueRoutes}

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
  val messageToSend = conf.getString("custom.messages.body")

  // actors
  val queueConnector = system.actorOf(QueueConnector.props(queueName), "queue-connector")
  val stockPriceConnector = system.actorOf(StockPriceConnector.props(apiKey), "stock-price-connector")
  val stockPriceWorker = system.actorOf(MessageWorker.props(queueConnector, messageToSend), "message-worker")

  // route definitions
  val queueRoutes = new QueueRoutes(queueConnector, stockPriceConnector)
  val monitoringRoutes = new MonitoringRoutes()

  // merge all routes here
  def allRoutes = {
    queueRoutes.routes ~
    monitoringRoutes.routes
  }

  Http().bindAndHandle(allRoutes, "0.0.0.0", 5000)
}
