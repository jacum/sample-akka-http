package com.vtex.akkahttpseed.routes

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout
import scala.concurrent.duration._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
/**
  * Created by felipe.almeida@vtex.com.br on 16/06/16.
  */
class MonitoringRoutes(implicit system: ActorSystem) {

  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(10.seconds)
  implicit val mat = ActorMaterializer(ActorMaterializerSettings(system))

  def routes = {
    path("healthcheck") {
      get {
        complete{
          "OK"
        }
      }
    }
  }

}
