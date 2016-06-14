package com.queirozf.akkahttpseed.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.ask
import com.queirozf.akkahttpseed.actors.QueueConnector

/**
  * Created by felipe on 13/06/16.
  */
class QueueRoutes(queueConnector: ActorRef)(implicit system: ActorSystem) {

  implicit val ec = system.dispatcher
  //  implicit val mat = ActorMaterializer(ActorMaterializerSettings(system))

  def routes = {
    pathPrefix("queue") {
      pathEndOrSingleSlash {
        post {
          sendMessage
        } ~
        get {
          receiveMessage
        }
      }
    }
  }

  private def sendMessage = {
    extract(context => context.request.entity) { entity =>
      complete {
        Unmarshal(entity).to[String].flatMap { message =>
          (queueConnector ? QueueConnector.SendMessage(message)).mapTo[Option[Unit]].map {
            case Some(_) => HttpResponse(StatusCodes.Accepted)
            case None => HttpResponse(StatusCodes.ServiceUnavailable)
          }
        }
      }
    }
  }

  private def receiveMessage = {
    complete {
      (queueConnector ? QueueConnector.ReceiveMessage).mapTo[Option[String]].map {
        case Some(message) => HttpResponse(entity = HttpEntity(message))
        case None => HttpResponse(StatusCodes.NotFound)
      }
    }
  }

}
