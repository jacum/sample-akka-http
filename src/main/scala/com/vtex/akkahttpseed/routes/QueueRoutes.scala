package com.vtex.akkahttpseed.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout
import com.vtex.akkahttpseed.actors.QueueConnector.SendMessageResultContainer
import com.vtex.akkahttpseed.actors.{QueueConnector, StockPriceConnector}
import com.vtex.akkahttpseed.models.DailyQuoteResult
import com.vtex.akkahttpseed.models.forms.GetQuoteModel
import com.vtex.akkahttpseed.models.marshallers.Implicits._
import com.vtex.akkahttpseed.models.response.QueueMessage

import scala.concurrent.Future
import scala.concurrent.duration._

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
      }
    } ~
      path("readFromQueue" / IntNumber) { limit =>
        get {
          complete {
            receiveMessages(limit)
          }
        }
      } ~
      path("readFromQueue") {
        get {
          complete {
            receiveMessages(10)
          }
        }
      }
  }


  /**
    * This method sends a quote (stock price) from the company identified by $model.ticker,
    * at a random date.
    *
    * Note that this method uses `map` and `flatMap` to resolve futures, options, tries and other monads
    *
    * @param model
    * @return
    */
  private def sendMessage(model: GetQuoteModel): Future[HttpResponse] = {

    val askResult = (stockPriceConnector ? StockPriceConnector.GetQuote(model.ticker, model.day, model.month, model.year))
      .mapTo[Option[DailyQuoteResult]]
    val output = askResult.flatMap {

      // value of a stock can be empty on weekends
      case Some(result) if result.dataset.data.nonEmpty => {
        val ticker = model.ticker
        val dateFormated = s"${model.day}-${model.month}-${model.year}"
        val (date, value) = result.dataset.data.head
        val queueMessage = s"value for $ticker on $dateFormated was USD $value"
        val sendResult = (queueConnector ? QueueConnector.SendMessage(queueMessage)).mapTo[SendMessageResultContainer]
        val output = sendResult.flatMap { case resultContainer => Marshal(QueueMessage(resultContainer.messageId, None)).to[HttpResponse] }
        output
      }
      case _ => Future.successful(HttpResponse(StatusCodes.NotFound, entity = HttpEntity("Failed to find stock price")))
    }
    output
  }


  /**
    * This method returns up to `upTo` messages, that may be available for reading in the queue.
    *
    * @param upTo
    * @return
    */
  private def receiveMessages(upTo: Int) = {
    val result = (queueConnector ? QueueConnector.ReceiveMessages(Some(upTo))).mapTo[List[QueueMessage]]
    result
  }

}