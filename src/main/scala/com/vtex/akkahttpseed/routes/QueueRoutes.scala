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
import com.vtex.akkahttpseed.models.marshalling.CustomMarshallers._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

class QueueRoutes(queueConnector: ActorRef, stockPriceConnector: ActorRef)
                 (implicit ec: ExecutionContextExecutor) {

  implicit val timeout = Timeout(10.seconds)

  def routes: Route = {
    pathPrefix("queue") {
      path("stock") {
        post {
          entity(as[GetQuoteModel]) { formModel =>
            complete {
              sendMessage(formModel)
            }
          }
        }
      } ~
        parameter('limit.as[Int]) { limit =>
          get {
            complete {
              receiveMessages(limit)
            }
          }
        }
    }
  }


  /**
    * This method sends a quote (stock price) from the company identified by $model.ticker,
    * on the date specified
    *
    * Future is a Monad; you can transform and compose Monads for an easier computation flow without
    * complex errors handlers and split in logic. You can transform a Monad using map / flatMap as below.
    *
    * Monads enable you to focus on "Happy Paths" (business logic) rather than error handling.
    *
    * You can handle errors at a single point; we do so at the route level
    *
    * @param model
    * @return
    */
  private def sendMessage(model: GetQuoteModel): Future[QueueMessage] = {

    val askResult = (stockPriceConnector ? StockPriceConnector.GetQuote(model.ticker, model.day, model.month, model.year))
      .mapTo[Option[DailyQuoteResult]]
    val output: Future[QueueMessage] = askResult.flatMap {
      // value of a stock is empty on weekends
      case Some(result) if result.dataset.data.nonEmpty => {
        val ticker = model.ticker
        val dateFormatted = s"${model.day}-${model.month}-${model.year}"
        val (date, value) = result.dataset.data.head
        val queueMessage = s"Message sent manually: value for $ticker on $dateFormatted was USD $value"
        val sendResult = (queueConnector ? QueueConnector.SendMessage(queueMessage)).mapTo[SendMessageResultContainer]

        sendResult.map { case msgRes => QueueMessage(msgRes.messageId, None) }
      }
      case _ => {
        // transform result into a custom failure, no need to throw an exception
        // note we are not using `throw`
        val exception = new ExternalResourceNotFoundException("Failed to find stock price. Stock does not exist or data is empty.")

        Future.failed(exception)
      }
    }
    output
  }


  /**
    * This method returns up to `upTo` messages, which may be available for reading in the queue.
    *
    * @param upTo
    * @return
    */
  private def receiveMessages(upTo: Int) = {
    val result = (queueConnector ? QueueConnector.ReceiveMessages(Some(upTo))).mapTo[List[QueueMessage]]
    result
  }


}