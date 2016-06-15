package com.vtex.akkahttpseed.actors

import java.util.concurrent.{Future => JFuture}

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.amazonaws.services.sqs.model.{GetQueueUrlResult, SendMessageRequest}
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSAsyncClient, AmazonSQSClient}

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

  var client: AmazonSQSClient = null

  def receive = {

    case InitClient => {
      client = new AmazonSQSClient()
    }

    case SendMessage(messageToSend: String) => {
      sendMessage(messageToSend) pipeTo sender()
    }

    case ReceiveMessages => {
      receiveMessages pipeTo sender()
    }
  }

  override def preStart = {
    self ! InitClient
  }

  private def sendMessage(message: String): Future[Try[String]] = {
    Future {
      for {
        getUrlResult <- Try(client.getQueueUrl(queueName))
        sendMessageResult <- Try(client.sendMessage(getUrlResult.getQueueUrl, message))
      } yield sendMessageResult.getMessageId
    }
  }

  private def receiveMessages: Future[List[String]] = {
    Future(Some("foo"))
  }

}

object QueueConnector {

  case object InitClient

  case class SendMessage(message: String)

  case object ReceiveMessages

}
