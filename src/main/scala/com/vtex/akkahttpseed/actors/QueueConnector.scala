package com.vtex.akkahttpseed.actors

import akka.actor.{Actor, ActorLogging, Props, Stash}
import akka.pattern.pipe
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model._
import com.vtex.akkahttpseed.actors.QueueConnector._
import com.vtex.akkahttpseed.models.QueueMessage
import com.vtex.akkahttpseed.utils.AWSAsyncHandler

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Companion object for the Actor
  *
  * props is an actor factory; it is located here to avoid serialization and race issues
  * since actor creation is async and with location transparent
  *
  * case objects / case classes are messages that this actor can handle
  *
  * This structure follows the Akka Recommended Practices for Actors
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
  * The initialization of this Actor depends on an async call that can take time to initialize.
  * No messages are lost during the uninitialized state, because of stashing
  * http://doc.akka.io/docs/akka/current/scala/actors.html#Stash
  *
  * @param queueName
  */
class QueueConnector(val queueName: String) extends Actor with ActorLogging with Stash {


  import context.dispatcher

  // the `log` variable is provided by the `ActorLogging` trait
  implicit val logging = log

  val retryInitDelay = 10.seconds

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
    *
    * More at: http://doc.akka.io/docs/akka/current/scala/actors.html#Stash
    *
    * @return
    */
  private def uninitialized: Receive = {

    // You cannot use objects with a live connection inside a message between actors (sqsClient) because
    // these are not serializable; for this reason sqsClient is available as a mutable variable (var rather
    // than val)
    case CompleteInitialize(queueUrl) => {

      // Changing state here is safe (because it's being done via message passing), but NOT
      // inside a Future because of the possibility of racing conditions
      //
      // http://doc.akka.io/docs/akka/current/general/jmm.html#Actors_and_shared_mutable_state

      unstashAll()
      context.become(initialized(queueUrl))
      log.info("initialized, messages unstashed")
    }
    case TryInitialize => tryInitQueueURL()

    case _ => {
      // stash() saves messages for future processing
      stash()
      log.info("Message saved to Stash to be processed when the actor state is initialized")
    }
  }

  /**
    * In the `initialized` state, this actor only responds to queue messages (SendMessage, ReceiveMessages) in this state
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

  /** *
    * Resolution of the Queue URL is done asynchronously to illustrate an async initialization behavior.
    * You can use a static url without this call.
    */
  def tryInitQueueURL() {

    sqsClient = new AmazonSQSAsyncClient()

    // Read more about the custom AWSAsyncHandler inside its source code.
    val queueUrlResult = new AWSAsyncHandler[GetQueueUrlRequest, GetQueueUrlResult]()
    sqsClient.getQueueUrlAsync(queueName, queueUrlResult)
    queueUrlResult.future.onComplete {

      case Success((request, result)) => {

        // WARNING, Never change any state of the actor inside a Future because of race conditions.
        // State changes need to be done via message processing, in this case within the
        // "CompleteInitialize" message handler
        //
        // http://doc.akka.io/docs/akka/current/general/jmm.html#Actors_and_shared_mutable_state

        self ! CompleteInitialize(result.getQueueUrl)

        // actor can´t continue if it is unable to initialize; it will retry later
      }
      case Failure(ex) => {
        log.warning("Initialization error, retry in {}", retryInitDelay)
        context.system.scheduler.scheduleOnce(retryInitDelay, self, TryInitialize)
      }

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

