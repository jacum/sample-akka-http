package com.vtex.akkahttpseed.actors

import java.util.concurrent.{Future => JFuture}

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.amazonaws.services.sqs.model.{GetQueueUrlResult, ReceiveMessageRequest, SendMessageRequest}
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSAsyncClient, AmazonSQSClient}
import com.vtex.akkahttpseed.models.response.QueueMessage

import scala.compat.java8.FutureConverters._
import scala.concurrent.java8.FuturesConvertersImpl
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.Future
import scala.util.Try

/**
  * Created by felipe on 12/06/16.
  */
class QueueConnector(val queueName: String) extends Actor with ActorLogging {

  import QueueConnector._
  import context.dispatcher

  implicit val system = context.system
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))

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
    * In the uninitialized stated, this actor only responds to InitClient
    *
    * @return
    */
  private def uninitialized: Receive = {

    case InitClient => {

      val sqsClient = new AmazonSQSClient()

      val tryQueueUrl = Try(sqsClient.getQueueUrl(queueName).getQueueUrl)

      context.become(initialized(sqsClient, tryQueueUrl))

    }
  }

  /**
    * In the `initialized` state, this actor responds to these messages
    *
    * @param client
    * @param tryQueueUrl
    * @return
    */
  private def initialized(client: AmazonSQSClient, tryQueueUrl: Try[String]): Receive = {

    case SendMessage(messageToSend: String) => {
      sendMessageToQueue(client, tryQueueUrl, messageToSend) pipeTo sender()
    }

    case ReceiveMessages(upTo) => {
      receiveMessagesFromQueue(client, tryQueueUrl, upTo.getOrElse(10)) pipeTo sender()
    }

    case DeletMessage(receiptHandle) => {
      deleteMessage(client, tryQueueUrl, receiptHandle)
    }

  }

  private def sendMessageToQueue(client: AmazonSQSClient, tryQueueUrl: Try[String], message: String): Future[Try[String]] = {
    Future {
      for {
        queueUrl <- tryQueueUrl
        sendMessageResult <- Try(client.sendMessage(queueUrl, message))
      } yield sendMessageResult.getMessageId
    }
  }

  private def receiveMessagesFromQueue(
                                        client: AmazonSQSClient,
                                        tryQueueUrl: Try[String],
                                        upTo: Int): Future[Try[List[QueueMessage]]] = {

    Future {
      for {
        queueUrl <- tryQueueUrl
        receiveRequest = new ReceiveMessageRequest().withQueueUrl(queueUrl).withMaxNumberOfMessages(upTo)
        messageObjects <- Try(client.receiveMessage(receiveRequest).getMessages.asScala.toList)

        // underscore represents an unused result
        _ = messageObjects.foreach { message =>
          self ! DeletMessage(message.getReceiptHandle)
        }

      } yield messageObjects.map { obj =>
        QueueMessage(obj.getMessageId, Some(obj.getBody))
      }.take(upTo)
    }
  }

  private def deleteMessage(client: AmazonSQSClient, tryUrl: Try[String], receiptHandle: String): Future[Try[Unit]] = {
    Future {
      for {
        queueUrl <- tryUrl
        // underscore represents an unused result
        _ <- Try(client.deleteMessage(queueUrl, receiptHandle))
      } yield ()
    }
  }

}

object QueueConnector {

  case object InitClient

  case class SendMessage(message: String)

  case class ReceiveMessages(upTo: Option[Int])

  case class DeletMessage(receiptHandle: String)

}
