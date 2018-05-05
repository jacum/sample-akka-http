package io.dhlparcel.sampleservice

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.NoContent
import akka.http.scaladsl.server.{ExceptionHandler, HttpApp, Route}
import akka.http.scaladsl.settings.{RoutingSettings, ServerSettings}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.dhlparcel.docs.DocResources
import io.dhlparcel.metrics.{MetricExtension, MetricsDirectives, MonitoringResources}

/** Uses strict logging to init logging subsystem before starting main parts */
object Boot extends StrictLogging {
  def main(args: Array[String]): Unit = {
    logger.info("Starting Sample API")

    try {
      ServiceAPI.start()
    } catch {
      case e: Throwable =>
        logger.error(s"Failed to start Example API! Error: $e", e)
    }
  }
}


object ServiceAPI extends StrictLogging {

  def start() = {
    val config = ConfigFactory.load().getConfig("io.dhlparcel.example")
    val system = ActorSystem("io-dhlparcel-example", config)
    val server = new ServiceAPI(config)(system)
    server.startServer()
  }

}

class ServiceAPI(config: Config)(implicit system: ActorSystem)
  extends HttpApp with StrictLogging with FailFastCirceSupport {

  private implicit val routingSettings: RoutingSettings = RoutingSettings(system.settings.config)
  private implicit val exceptionHandler: ExceptionHandler = ExceptionHandler.default(routingSettings)
 // private val rejectionHandler = RejectionHandler.default

  private val metricSettings = MetricExtension(system).metricSettings

  private val documentationRoute = new DocResources(Paths.get(config getString "docs").normalize.toAbsolutePath).route
  private val monitoringRoute = new MonitoringResources(metricSettings.reporter.report).route


  private val metricsDirectives =
    new MetricsDirectives(metricSettings.metrics.httpMetrics)

  private def unsealedRoutes: Route =
      monitoringRoute ~
      documentationRoute ~
      manifest ~
      noIcon

  private def manifest =
    path("api" / "sample" / "v1") {
      complete {
        val pkg = getClass.getPackage
        val title = Option(pkg.getImplementationTitle) getOrElse "Sample API"
        val version = Option(pkg.getImplementationVersion) getOrElse "Development"
        Map(
          "title" -> title,
          "version" -> version,
          "api" -> "1"
        )
      }
    }

  private def noIcon =
    get {
      path("favicon.ico") {
        complete {
          NoContent
        }}}

  val routes: Route = metricsDirectives.collectMetrics(Route.seal(unsealedRoutes))

  def startServer(): Unit = {
    this.startServer("0.0.0.0", config getInt "port", ServerSettings(ConfigFactory.load), system)
  }

}
