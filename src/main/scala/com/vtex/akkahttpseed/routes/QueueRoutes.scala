package com.vtex.akkahttpseed.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshalling.Marshal
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
import com.vtex.akkahttpseed.models.response.QueueMessage

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}

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
    path("writeToQueue") {
      post {
        entity(as[GetQuoteModel]) { formModel =>
          complete {
            sendMessage(formModel)
          }
        }
      } ~
      path("readFromQueue") {
        get {
          complete{
            receiveMessages
          }
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

  private def sendMessage(model: GetQuoteModel) = {

    val ticker = model.ticker
    val year = 2015
    val month = Random.nextInt(11) + 1
    val day = Random.nextInt(27) + 1 // 2015 is not a leap year so maximum is 28

    (stockPriceConnector ? StockPriceConnector.GetQuote(ticker, day, month, year)).mapTo[Try[Option[DailyQuoteResult]]].flatMap {
      case Success(maybeResult) => {
        maybeResult match {
          case Some(result) => {
            if (result.dataset.data.nonEmpty) {
              val (date, value) = result.dataset.data.head
              val queueMessage = s"value for $date was $value"

              (queueConnector ? QueueConnector.SendMessage(queueMessage)).mapTo[Try[String]].flatMap {
                case Success(messageId) => Marshal(QueueMessage(messageId)).to[HttpResponse]
                case Failure(e) => Future(HttpResponse(StatusCodes.InternalServerError, entity = HttpEntity(e.getMessage)))
              }
            } else {
              Future(HttpResponse(StatusCodes.NotFound, entity = HttpEntity(s"""Failed to find stock price for "$ticker" on $day-$month-$year""")))
            }
          }
          case None => Future(HttpResponse(StatusCodes.NotFound, entity = HttpEntity(s"""Failed to find stock price for "$ticker" on $day-$month-$year""")))
        }
      }
      case Failure(e) => Future(HttpResponse(StatusCodes.InternalServerError, entity = HttpEntity(e.getMessage)))
    }
  }

  private def receiveMessages = {
    (queueConnector ? QueueConnector.ReceiveMessages).mapTo[List[String]].flatMap { messages =>
      if(messages.nonEmpty){
        ???
      }else{
        Marshal(List.empty[String]).to[HttpResponse]
      }
    }
  }
}
