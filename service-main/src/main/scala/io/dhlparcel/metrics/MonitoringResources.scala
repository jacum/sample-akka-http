package io.dhlparcel.metrics

import akka.http.scaladsl.coding.Gzip
import akka.http.scaladsl.marshalling.Marshaller.byteStringMarshaller
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives.{complete, encodeResponseWith, get, path, _}
import akka.http.scaladsl.server.Route
import akka.util.ByteString
import akka.http.scaladsl.server.Directives._

object MonitoringResources {
  class InvalidContentTypeException(description: String)
    extends Exception(
      s"Invalid content type for metrics resource: '${ Report.ContentType }'. $description")

  private val TextReportContentType =
    ContentType.parse(Report.ContentType).fold(
      err => throw new InvalidContentTypeException(err.map(_.formatPretty).mkString),
      identity
    )

  implicit val ReportMarshaller: ToEntityMarshaller[Report] =
    byteStringMarshaller(TextReportContentType).compose[Report](r => ByteString(r.text))

}

class MonitoringResources(
  metricsReport: => Report
) {
  import MonitoringResources._

  def route: Route =
    check ~
      metrics

  private def check =
    get {
      path("health" / "check") {
        complete((OK, "OK"))
      }
    }

  private def metrics =
    get {
      path("metrics") {
        encodeResponseWith(Gzip) {
          complete(metricsReport)
        }
      }
    }
}
