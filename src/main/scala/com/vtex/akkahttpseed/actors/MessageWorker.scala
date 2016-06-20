package com.vtex.akkahttpseed.actors

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

/**
  * Companion object for the Actor
  *
  * props is the actor factory that is safer to be here to not get in race conditions and serialization issues
  * since actors creations are async
  *
  * case object / case class are messages that this actor can handle
  *
  * This structure follow the Akka Recommended Practices for Actors
  * http://doc.akka.io/docs/akka/current/scala/actors.html#Recommended_Practices
  *
  */
object MessageWorker {

  def props(queueConnector: ActorRef, messageToSend: String): Props = Props(new MessageWorker(queueConnector, messageToSend))

  case class SendMessageToQueue(message: String)

}

/**
  * This actor once started will periodically tell the QueueConnector Actor to write a message to the queue.
  * This is done using scheduling.
  * http://doc.akka.io/docs/akka/current/scala/howto.html#scheduling-periodic-messages
  */
class MessageWorker(queueConnector: ActorRef, messageBody: String) extends Actor with ActorLogging {

  import MessageWorker._

  implicit val system = context.system
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  implicit val ec = context.dispatcher

  // The Actor QueueConnector will receive the first messages before it become ready and will handle it properly
  // without loosing any message. Read more inside the QueueConnector source code.
  context.system.scheduler.schedule(0.millis, 10000.millis, self, SendMessageToQueue(messageBody))

  def receive = {

    case SendMessageToQueue(message) => {

      log.info("Sending message to queue")

      val actualMessage = "Message sent automatically: '" + message + "' received at " + ZonedDateTime.now.format(DateTimeFormatter.ISO_INSTANT)

      queueConnector ! QueueConnector.SendMessage(actualMessage)
    }

  }

}

