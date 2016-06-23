package com.vtex.akkahttpseed.actors

import akka.actor.{Actor, ActorLogging, Props, Stash}
import akka.pattern.pipe
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model._
import com.vtex.akkahttpseed.actors.QueueConnector._
import com.vtex.akkahttpseed.models.response.QueueMessage
import com.vtex.akkahttpseed.utils.aws.AWSAsyncHandler

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

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

  //factory
  def props(queueName: String): Props = Props(new QueueConnector(queueName))

  //messages
  private case object TryInitialize

  private case class CompleteInitialize(queueUrl: String)

  case class SendMessage(message: String)

  case class ReceiveMessages(upTo: Option[Int])

  //results
  case class SendMessageResultContainer(messageId: String)

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


  import context.dispatcher

  implicit val logging = log

  val retryInitDelay = 10 seconds

  // var is mutable , val is immutable
  // This client is mutable because extract the current available credentials on creation.
  // If the credential is not valid, it is necessary to create another client.
  var sqsClient: AmazonSQSAsyncClient = null

  self ! TryInitialize


  // This actor starts off using the `uninitialized`
  def receive: Receive = uninitialized

  /**
    * Uninitialized state, actor is not immediately ready because of async operations.
    * All unexpected messages in this state will be stashed to be processed later when the Actor is initialized.
    * More at: http://doc.akka.io/docs/akka/current/scala/actors.html#Stash
    *
    * @return
    */
  private def uninitialized: Receive = {

    // You cannot use objects with a live connection inside a message between actors (sqsClient) because
    // all messages are serialized and the connection will be lost, for this reason sqsClient is available class in scope
    case CompleteInitialize(queueUrl) =>

      // Change state is safe here (message processing), but never inside a Future because of racing conditions
      // http://doc.akka.io/docs/akka/current/general/jmm.html#Actors_and_shared_mutable_state

      unstashAll()
      context.become(initialized(queueUrl))
      log.info("initialized, messages unstashed")

    case TryInitialize =>
      tryInitQueueURL()

    case _ =>
      stash()
      log.info("Message saved to Stash to be processed when the actor state is initialized")
  }

  /** *
    * The resolve of Queue URL is done asynchronously just to illustrate an async initialization behavior.
    * You can use a static url without this call.
    */
  def tryInitQueueURL() {

    sqsClient = new AmazonSQSAsyncClient()

    // Read more about the custom AWSAsyncHandler inside it´s source code.
    val queueUrlResult = new AWSAsyncHandler[GetQueueUrlRequest, GetQueueUrlResult]()
    sqsClient.getQueueUrlAsync(queueName, queueUrlResult)
    queueUrlResult.future.onComplete {

      case Success((request, result)) =>

        // WARNING, Never change any state of the actor inside a Future because of race conditions.
        // The state change need to be done inside the message processing, in this case within the
        // "CompleteInitialize" message handling
        // http://doc.akka.io/docs/akka/current/general/jmm.html#Actors_and_shared_mutable_state

        self ! CompleteInitialize(result.getQueueUrl)

      // actor can´t continue if it is unable to initialize, but it will retry later
      case Failure(ex) =>
        log.warning("Initializtion error, retry in {}", retryInitDelay)
        context.system.scheduler.scheduleOnce(retryInitDelay, self, TryInitialize)

    }

  }


  /**
    * In the `initialized` state, this actor only respond to queue messages (SendMessage, ReceiveMessages) in this state
    *
    * @param queueUrl
    * @return
    */
  private def initialized(queueUrl: String): Receive = {

    case SendMessage(messageToSend: String) => {

      log.info("Sending message to SQS")

      // pipeTo is a pattern to forward a Future to the sender without worry about change of sender.
      // Since the bellow operation is async, the Actor is ready to receive another message from
      // another sender even if the result is not complete.
      // http://doc.akka.io/docs/akka/current/scala/actors.html#Ask__Send-And-Receive-Future

      sendMessageToQueue(queueUrl, messageToSend) pipeTo sender()
    }

    case ReceiveMessages(upTo) => {
      log.info("Reading messages from SQS")
      receiveMessagesFromQueue(queueUrl, upTo.getOrElse(10)) pipeTo sender()
    }

  }

  private def sendMessageToQueue(queueUrl: String, message: String): Future[SendMessageResultContainer] = {
    val sendResult = new AWSAsyncHandler[SendMessageRequest, SendMessageResult]()
    val result = sendResult.future.map { case (sendMessageRequest, sendMessageResult) => SendMessageResultContainer(sendMessageResult.getMessageId) }
    sqsClient.sendMessageAsync(queueUrl, message, sendResult)
    result
  }

  private def receiveMessagesFromQueue(queueUrl: String, upTo: Int): Future[List[QueueMessage]] = {

    // Read more about the custom AWSAsyncHandler inside the source code.
    val receiveResultHandler = new AWSAsyncHandler[ReceiveMessageRequest, ReceiveMessageResult]()

    val receiveRequest = new ReceiveMessageRequest().withQueueUrl(queueUrl).withMaxNumberOfMessages(upTo)
    sqsClient.receiveMessageAsync(receiveRequest, receiveResultHandler)

    val output = receiveResultHandler.future.map {
      case (_, receiveResult) => {
        val messages = receiveResult.getMessages.asScala.toList
        // delete messages
        val deleteEntries = messages.map(msg => new DeleteMessageBatchRequestEntry(msg.getMessageId, msg.getReceiptHandle))
        if (deleteEntries.size > 0) {
          val deleteRequestBatch = new DeleteMessageBatchRequest(queueUrl, deleteEntries.asJava)
          val deleteResultHandler = new AWSAsyncHandler[DeleteMessageBatchRequest, DeleteMessageBatchResult]
          sqsClient.deleteMessageBatchAsync(deleteRequestBatch, deleteResultHandler)
          // just to show that the messages are deleted asynchronously
          deleteResultHandler.future.onSuccess { case (req, res) => log.info("messages deleted: {}", res.getSuccessful.asScala.mkString(",")) }
        }
        // return messages
        messages.map(msg => QueueMessage(msg.getMessageId, Some(msg.getBody)))
      }
    }

    output
  }

}

