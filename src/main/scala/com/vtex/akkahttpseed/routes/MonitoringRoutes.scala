package com.vtex.akkahttpseed.routes

import akka.http.scaladsl.server.Directives._

class MonitoringRoutes {

  def routes = {
    path("healthcheck") {
      get {
        complete {
          "OK"
        }
      }
    }
  }

}
