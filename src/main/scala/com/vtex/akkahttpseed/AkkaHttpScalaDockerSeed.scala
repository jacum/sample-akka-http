package com.vtex.akkahttpseed

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.config.ConfigFactory
import com.vtex.akkahttpseed.actors.{MessageWorker, QueueConnector, StockPriceConnector}
import com.vtex.akkahttpseed.models.errors.{ExternalResourceException, ExternalResourceNotFoundException}
import com.vtex.akkahttpseed.routes.{MonitoringRoutes, QueueRoutes}

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

  implicit def myExceptionHandler: ExceptionHandler = customGlobalErrorHandler


  // merge all routes here
  def allRoutes = {
    queueRoutes.routes ~
      monitoringRoutes.routes
  }

  Http().bindAndHandle(allRoutes, "0.0.0.0", 5000)


  // When handling and completing errors as result like that
  // you have the option (like bellow) to not log errors
  // or log with a lower level when a failure is usually
  // caused is the user calling the API
  def customGlobalErrorHandler() = ExceptionHandler {
    case ex: ExternalResourceNotFoundException =>
      extractUri { uri =>
        // no errors will be logged here
        complete(HttpResponse(NotFound, entity = ex.message))
      }
    case ex: ExternalResourceException => {
      // a WARN will be logged instead of an error
      system.log.warning(ex.message)
      complete(HttpResponse(BadGateway, entity = ex.message))
    }

    // This behave as a pipeline, errors that are not handled above will be passed forward up to the route
    // default is always log the error with InternalServerError and no body
  }


}
