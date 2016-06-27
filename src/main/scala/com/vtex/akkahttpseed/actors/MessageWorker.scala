package com.vtex.akkahttpseed.actors

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.vtex.akkahttpseed.actors.MessageWorker.SendMessageToQueue

import scala.concurrent.duration._

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

  import context.dispatcher

  // The Actor QueueConnector will receive the first message before it becomes ready; however,
  // this is not a problem since we are using `stashing`
  //
  // Read more inside QueueConnector source code.
  context.system.scheduler.schedule(0.millis, 10000.millis, self, SendMessageToQueue(messageBody))

  def receive = {

    case SendMessageToQueue(message) => {

      log.info("Sending message to QueueConnector")

      val actualMessage = "Message sent automatically: '" + message + "' received at " + ZonedDateTime.now.format(DateTimeFormatter.ISO_INSTANT)

      queueConnector ! QueueConnector.SendMessage(actualMessage)
    }

  }

}

