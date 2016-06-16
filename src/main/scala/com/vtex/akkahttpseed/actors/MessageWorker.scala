package com.vtex.akkahttpseed.actors

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import scala.concurrent.duration._

/**
  * This actor, once started, will periodically ask the queue connector (actor that controls the
  * queue system) to write a message to the queue. This is done using scheduling; read more
  * on this link: http://doc.akka.io/docs/akka/current/scala/howto.html#scheduling-periodic-messages
  */
class MessageWorker(queueConnector: ActorRef, messageBody: String) extends Actor with ActorLogging {

  import MessageWorker._
  import context.dispatcher

  implicit val system = context.system
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  override def preStart = {
    log.debug("Worker will start scheduling")

    // schedule the delivery of messages to this very actor (self) every 5 seconds
    context.system.scheduler.schedule(500.millis, 5000.millis, self, SendMessageToQueue(messageBody))
  }

  def receive = {

    case SendMessageToQueue(message) => {

      log.debug("Worker will send message to queue")

      val actualMessage = "Message sent automatically: '" + message + "' received at " + ZonedDateTime.now.format(DateTimeFormatter.ISO_INSTANT)

      queueConnector ! QueueConnector.SendMessage(actualMessage)
    }

  }

}

object MessageWorker {

  case object Initialize

  case class SendMessageToQueue(message: String)

}
