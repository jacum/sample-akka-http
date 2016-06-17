package com.vtex.akkahttpseed.actors

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import scala.concurrent.duration._

/**
  * Companion object for the Actor
  *
  * props is the actor factoring that is safer to be in a companion object to not get in serialization and race issues
  * since actors creations are async and location transparency
  *
  * case object / case class are messages that this actor can handle
  *
  * This structure follow the Akka Recommended Practices for Actors
  * http://doc.akka.io/docs/akka/current/scala/actors.html#Recommended_Practices
  *
  */
object MessageWorker {

  def props(queueConnector: ActorRef, messageToSend: String): Props = Props(new MessageWorker(queueConnector, messageToSend))

  case object Initialize

  case class SendMessageToQueue(message: String)

}

/**
  * This actor, once started, will periodically tell the queue connector (actor that controls the
  * queue system) to write a message to the queue. This is done using scheduling; read more
  * on this link: http://doc.akka.io/docs/akka/current/scala/howto.html#scheduling-periodic-messages
  */
class MessageWorker(queueConnector: ActorRef, messageBody: String) extends Actor with ActorLogging {

  import MessageWorker._
  import context.dispatcher

  implicit val system = context.system
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  context.system.scheduler.schedule(500.millis, 5000.millis, self, SendMessageToQueue(messageBody))

  def receive = {

    case SendMessageToQueue(message) => {

      log.debug("Worker will send message to queue")

      val actualMessage = "Message sent automatically: '" + message + "' received at " + ZonedDateTime.now.format(DateTimeFormatter.ISO_INSTANT)

      queueConnector ! QueueConnector.SendMessage(actualMessage)
    }

  }

}

