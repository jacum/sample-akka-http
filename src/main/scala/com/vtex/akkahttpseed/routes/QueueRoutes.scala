package com.vtex.akkahttpseed.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.ask
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout
import com.vtex.akkahttpseed.actors.{QueueConnector, StockPriceConnector}
import com.vtex.akkahttpseed.models.DailyQuoteResult
import com.vtex.akkahttpseed.models.forms.GetQuoteModel
import com.vtex.akkahttpseed.models.marshallers.Implicits._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

/**
  * Created by felipe on 13/06/16.
  */
class QueueRoutes(
                   queueConnector: ActorRef,
                   stockPriceConnector: ActorRef)(implicit system: ActorSystem) {

  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(10.seconds)
  implicit val mat = ActorMaterializer(ActorMaterializerSettings(system))

  def routes: Route = {
    pathPrefix("queue") {
      pathPrefix("messages") {
        pathEndOrSingleSlash {
          post {
            entity(as[GetQuoteModel]) { formModel =>
              complete {
                sendMessage(formModel)
              }
            }
          } ~
            get {
              complete("received message")
            }
        } ~
          path("initWorker") {
            complete("initialized worker")
          } ~
          path("stopWorker") {
            complete("stopped worked")
          }
      }
    }
  }

  private def sendMessage(model: GetQuoteModel) = {

    val ticker = model.ticker
    val year = 2015
    val month = Random.nextInt(11) + 1
    val day = Random.nextInt(27) + 1 // 2015 is not a leap year so maximum is 28

    (stockPriceConnector ? StockPriceConnector.GetQuote(ticker, day, month, year)).mapTo[Option[DailyQuoteResult]].flatMap {
      case Some(result) => {

        val (date, value) = result.dataset.data.head
        val queueMessage = s"value for $date was $value"

        (queueConnector ? QueueConnector.SendMessage(queueMessage)).mapTo[Option[Unit]].map {
          case Some(_) => HttpResponse(StatusCodes.Accepted)
          case None => HttpResponse(StatusCodes.ServiceUnavailable)
        }
      }
      case None => Future(HttpResponse(StatusCodes.BadRequest, entity = HttpEntity("quote not found")))
    }

  }

  //  private def receiveMessage = {
  //    complete {
  //      (queueConnector ? QueueConnector.ReceiveMessage).mapTo[Option[String]].map {
  //        case Some(message) => HttpResponse(entity = HttpEntity(message))
  //        case None => HttpResponse(StatusCodes.NotFound)
  //      }
  //    }
  //  }

}
