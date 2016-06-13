package com.queirozf.akkahttpseed.actors

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import scala.concurrent.Future

/**
  * Created by felipe on 12/06/16.
  */
class QueueConnector(val queueName: String) extends Actor with ActorLogging {

  import QueueConnector._
  import context.dispatcher

  implicit val system = context.system
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  def receive = {

    case SendMessage(messageToSend: String) =>
      sendMessage(messageToSend) pipeTo sender()

    case ReceiveMessage =>
      receiveMessage pipeTo sender()

  }


  private def sendMessage(message: String): Future[Unit] = {
    ???
  }

  private def receiveMessage:Future[Option[String]] = {
    ???
  }

}

object QueueConnector {

  case class SendMessage(message: String)

  case object ReceiveMessage

}
