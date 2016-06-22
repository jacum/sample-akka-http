import akka.actor.Props
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.ConfigFactory
import com.vtex.akkahttpseed.actors.{MessageWorker, QueueConnector, StockPriceConnector}
import com.vtex.akkahttpseed.routes.{MonitoringRoutes, QueueRoutes}
import org.scalatest.{Matchers, WordSpec}

class RouteTesting extends WordSpec with Matchers with ScalatestRouteTest {

  implicit val ec = system.dispatcher

  val conf = ConfigFactory.load()
  val queueName = conf.getString("custom.queue.name")
  val apiKey = conf.getString("custom.stocks.api-key")
  val messageToSend = conf.getString("custom.messages.body")

  // actors
  val queueConnector = system.actorOf(QueueConnector.props(queueName), "queue-connector")
  val stockPriceConnector = system.actorOf(StockPriceConnector.props(apiKey), "stock-price-connector")
  val stockPriceWorker = system.actorOf(MessageWorker.props(queueConnector, messageToSend), "message-worker")

  // route definitions
  val queueRoutes = new QueueRoutes(queueConnector, stockPriceConnector)
  val monitoringRoutes = new MonitoringRoutes()

  // merge all routes here
  def allRoutes = {
    queueRoutes.routes ~
      monitoringRoutes.routes
  }

  "The service" should {
    "respond to healthcheck" in {
      Get("/healthcheck") ~> allRoutes ~> check {
        responseAs[String] shouldEqual "OK"
      }
    }
  }

}
