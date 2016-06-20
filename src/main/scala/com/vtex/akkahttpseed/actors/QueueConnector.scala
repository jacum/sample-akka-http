package com.vtex.akkahttpseed.actors

import akka.actor.{Actor, ActorLogging, Props, Stash}
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model._
import com.vtex.akkahttpseed.models.response.QueueMessage
import com.vtex.akkahttpseed.utils.aws.AWSAsyncHandler

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * Companion object for the Actor
  *
  * props is the actor factory that is safer to be here to not get in serialization and race issues
  * since actors creations are async and with location transparency
  *
  * case object / case class are messages that this actor can handle
  *
  * This structure follow the Akka Recommended Practices for Actors
  * http://doc.akka.io/docs/akka/current/scala/actors.html#Recommended_Practices
  *
  */
object QueueConnector {

  def props(queueName: String): Props = Props(new QueueConnector(queueName))

  case class CompleteInitialize(queueUrl: String)

  case class SendMessage(message: String)

  case class ReceiveMessages(upTo: Option[Int])

}

/**
  * This actor is like a SQS adapter. All operations with AWS SQS are done through here.
  *
  * It have two "states": uninitialized and initialized
  * Each state in a set of messages the actor handle in different ways.
  * http://doc.akka.io/docs/akka/current/scala/actors.html#Become_Unbecome
  *
  * For a more complex state flow, you can you can use Akka FSM (Finite State Machine), Not used in this example.
  * http://doc.akka.io/docs/akka/current/scala/fsm.html
  *
  * The initialization of this Actor depends of an async call that can take time to become initialized.
  * No messages are lost during the uninitialized state. This is done using Stash
  * http://doc.akka.io/docs/akka/current/scala/actors.html#Stash
  *
  * @param queueName
  */
class QueueConnector(val queueName: String) extends Actor with ActorLogging with Stash {

  import QueueConnector._
  import context.dispatcher

  implicit val logging = log
  implicit val system = context.system
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  val sqsClient = new AmazonSQSAsyncClient()

  initQueueURL()

  // This actor starts off using the `uninitialized`
  def receive: Receive = uninitialized

  /** *
    * The resolve of Queue URL is done asynchronously just to illustrate an async initialization behavior.
    * You can use a static url without this call.
    */
  def initQueueURL() {

    // Read more about the custom AWSAsyncHandler inside it´s source code.
    val queueUrlResult = new AWSAsyncHandler[GetQueueUrlRequest, GetQueueUrlResult]()

    sqsClient.getQueueUrlAsync(queueName, queueUrlResult)
    queueUrlResult.future.map {
      case (request, result) => {

        // WARNING, Never change any state of the actor inside a Future because of race conditions.
        // The state change need to be done inside the message processing like in CompleteInitialize
        // http://doc.akka.io/docs/akka/current/general/jmm.html#Actors_and_shared_mutable_state

        self ! CompleteInitialize(result.getQueueUrl)
      }
    }
  }


  /**
    * Uninitialized state, actor is not immediately ready because of async operations.
    * All unexpected messages in this state will be stashed to be processed later when the Actor is initialized.
    * More at: http://doc.akka.io/docs/akka/current/scala/actors.html#Stash
    *
    * @return
    */
  private def uninitialized: Receive = {

    case CompleteInitialize(queueUrl) =>

      // Change state is safe here (message processing), but never inside a Future because of racing conditions
      // http://doc.akka.io/docs/akka/current/general/jmm.html#Actors_and_shared_mutable_state

      unstashAll()
      context.become(initialized(queueUrl))
      log.info("initialized, messages unstashed")

    case _ =>

      stash()
      log.info("Message saved to Stash to be processed when the actor state is initialized")
  }

  /**
    * In the `initialized` state, this actor only respond to queue messages (SendMessage, ReceiveMessages) in this state
    *
    * @param queueUrl
    * @return
    */
  private def initialized(queueUrl: String): Receive = {

    case SendMessage(messageToSend: String) => {

      log.info("Sending message to queue")

      // pipeTo is a pattern to forward a Future to the sender without worry about change of sender.
      // Not applicable in this example (always the same sender) but is a recommended practice
      // http://doc.akka.io/docs/akka/current/scala/actors.html#Ask__Send-And-Receive-Future

      sendMessageToQueue(queueUrl, messageToSend) pipeTo sender()
    }

    case ReceiveMessages(upTo) => {
      log.info("Reading messages from queue")
      receiveMessagesFromQueue(queueUrl, upTo.getOrElse(10)) pipeTo sender()
    }

  }

  private def sendMessageToQueue(queueUrl: String, message: String): Future[String] = {
    val sendResult = new AWSAsyncHandler[SendMessageRequest, SendMessageResult]()
    val result = sendResult.future.map{case (sendMessageRequest, sendMessageResult) => sendMessageResult.getMessageId}
    sqsClient.sendMessageAsync(queueUrl, message, sendResult)
    result


//    Future {
//      for {
//        sendMessageResult <- Try(sqsClient.sendMessage(queueUrl, message))
//      } yield sendMessageResult.getMessageId
//    }

  }

  private def receiveMessagesFromQueue(queueUrl: String, upTo: Int): Future[Try[List[QueueMessage]]] = {

    // aws sdk in java use callback functions to return results, AWSAsyncHandler are handlers in scala that
    // expose scala futures for a more linear coding without the need of isolated callbacks
    val receiveResultHandler = new AWSAsyncHandler[ReceiveMessageRequest, ReceiveMessageResult]()

    val receiveRequest = new ReceiveMessageRequest().withQueueUrl(queueUrl).withMaxNumberOfMessages(upTo)
    sqsClient.receiveMessageAsync(receiveRequest, receiveResultHandler)

    receiveResultHandler.future.map {
      case (_, receiveResult) => {

        val messages = receiveResult.getMessages.asScala.toList

        val deleteEntries = messages.map(msg => new DeleteMessageBatchRequestEntry(msg.getMessageId, msg.getReceiptHandle))

        val deleteRequestBatch = new DeleteMessageBatchRequest(queueUrl, deleteEntries.asJava)

        val deleteResultHandler = new AWSAsyncHandler[DeleteMessageBatchRequest, DeleteMessageBatchResult]
        // we're not interested in the deletion result, we just want to delete
        sqsClient.deleteMessageBatchAsync(deleteRequestBatch, deleteResultHandler)

        val queueMessages = messages.map(msg => QueueMessage(msg.getMessageId, Some(msg.getBody)))

        Success(queueMessages)
      }
    }.recover {
      case NonFatal(nf) => Failure(nf)
    }

  }

}

