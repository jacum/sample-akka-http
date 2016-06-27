package com.vtex.akkahttpseed.routes

import akka.http.scaladsl.server.Directives._

class MonitoringRoutes {

  // note how no implicits are needed here, because we aren't using futures, just returning
  // a static value

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
