package com.vtex.akkahttpseed.routes

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.vtex.akkahttpseed.actors.QueueConnector.SendMessageResultContainer
import com.vtex.akkahttpseed.actors.{QueueConnector, StockPriceConnector}
import com.vtex.akkahttpseed.models.{DailyQuoteResult, GetQuoteModel, QueueMessage}
import com.vtex.akkahttpseed.models.errors.ExternalResourceNotFoundException
import com.vtex.akkahttpseed.models.marshallers.CustomMarshallers._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

class QueueRoutes(queueConnector: ActorRef, stockPriceConnector: ActorRef)
                 (implicit ec: ExecutionContextExecutor) {

  implicit val timeout = Timeout(10.seconds)

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
    * Future is a Monad. You can transform and compose Monads for an easier computation flow without
    * complex errors handlers and split in logic. You can transform a Monad using map / flatMap as bellow.
    * With Monads you can focus more on the "Happy Path" (business logic) and less with Errors.
    * You can worry about error in a single point, in this case is at the route level
    *
    * @param model
    * @return
    */
  private def sendMessage(model: GetQuoteModel): Future[QueueMessage] = {

    val askResult = (stockPriceConnector ? StockPriceConnector.GetQuote(model.ticker, model.day, model.month, model.year))
      .mapTo[Option[DailyQuoteResult]]
    val output: Future[QueueMessage] = askResult.flatMap {
      // value of a stock can be empty on weekends
      case Some(result) if result.dataset.data.nonEmpty => {
        val ticker = model.ticker
        val dateFormated = s"${model.day}-${model.month}-${model.year}"
        val (date, value) = result.dataset.data.head
        val queueMessage = s"value for $ticker on $dateFormated was USD $value"
        val sendResult = (queueConnector ? QueueConnector.SendMessage(queueMessage)).mapTo[SendMessageResultContainer]
        sendResult.map { case msgRes => QueueMessage(msgRes.messageId, None) }
      }
      // transform result in a custom failure, no need to throw an exception
      case _ => Future.failed(new ExternalResourceNotFoundException("Failed to find stock price. Stock not exists or data is empty."))
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