package com.vtex.akkahttpseed.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model._
import com.vtex.akkahttpseed.models.response.QueueMessage
import com.vtex.akkahttpseed.utils.aws.sqs.AWSAsyncHandler

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object QueueConnector {

  // actor "factory" - it's safer to do this in a companion object like this
  // so as to avoid serialization issues and race conditions, since
  // actor creation is asynchronous and location transparent
  // see also: http://doc.akka.io/docs/akka/current/scala/actors.html#props
  def props(queueName: String): Props = Props(new QueueConnector(queueName))

  // messages this actor supports:

  case object InitClient

  case class SendMessage(message: String)

  case class ReceiveMessages(upTo: Option[Int])

}

class QueueConnector(val queueName: String) extends Actor with ActorLogging {

  import QueueConnector._
  import context.dispatcher

  implicit val system = context.system
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  val sqsClient = new AmazonSQSAsyncClient()
  val queueUrlResult = new AWSAsyncHandler[GetQueueUrlRequest, GetQueueUrlResult]()
  sqsClient.getQueueUrlAsync(queueName, queueUrlResult)
  queueUrlResult.future.map {
    case (request, result) => {
      context.become(initialized(result.getQueueUrl))
    }
  }


  /**
    * This is called when this actor is started
    *
    */
  override def preStart: Unit = {
    self ! InitClient
  }


  // This actor starts off using the `uninitialized` method to respond to messages
  def receive: Receive = uninitialized

  /**
    * Uninitialized state, actor is not ready (because of async operations)
    *
    * @return
    */
  private def uninitialized: Receive = {
    case _ => log.warning("log not initialized")
  }

  /**
    * In the `initialized` state, this actor responds to these messages
    *
    * @param client
    * @param queueUrl
    * @return
    */
  private def initialized(queueUrl: String): Receive = {

    case SendMessage(messageToSend: String) => {
      log.debug("Queue connector will send message to queue")
      sendMessageToQueue(sqsClient, queueUrl, messageToSend) pipeTo sender()
    }

    case ReceiveMessages(upTo) => {
      log.debug("Queue connector will read messages from queue")
      receiveMessagesFromQueue(sqsClient, queueUrl, upTo.getOrElse(10)) pipeTo sender()
    }

  }

  private def sendMessageToQueue(
                                  client: AmazonSQSAsyncClient,
                                  queueUrl: String,
                                  message: String): Future[Try[String]] = {
    Future {
      for {
        sendMessageResult <- Try(client.sendMessage(queueUrl, message))
      } yield sendMessageResult.getMessageId
    }
  }

  private def receiveMessagesFromQueue(
                                        client: AmazonSQSAsyncClient,
                                        queueUrl: String,
                                        upTo: Int): Future[Try[List[QueueMessage]]] = {

    // aws sdk in java use callback functions to return results, AWSAsyncHandler are handlers in scala that
    // expose scala futures for a more linear coding without the need of isolated callbacks
    val receiveResultHandler = new AWSAsyncHandler[ReceiveMessageRequest, ReceiveMessageResult]()

    val receiveRequest = new ReceiveMessageRequest().withQueueUrl(queueUrl).withMaxNumberOfMessages(upTo)
    client.receiveMessageAsync(receiveRequest, receiveResultHandler)

    receiveResultHandler.future.map {
      case (_, receiveResult) => {

        val messages = receiveResult.getMessages.asScala.toList

        val deleteEntries = messages.map(msg => new DeleteMessageBatchRequestEntry(msg.getMessageId, msg.getReceiptHandle))

        val deleteRequestBatch = new DeleteMessageBatchRequest(queueUrl, deleteEntries.asJava)

        val deleteResultHandler = new AWSAsyncHandler[DeleteMessageBatchRequest, DeleteMessageBatchResult]
        // we're not interested in the deletion result, we just want to delete
        client.deleteMessageBatchAsync(deleteRequestBatch, deleteResultHandler)

        val queueMessages = messages.map(msg => QueueMessage(msg.getMessageId, Some(msg.getBody)))

        Success(queueMessages)
      }
    }.recover {
      case NonFatal(nf) => Failure(nf)
    }

  }

}

