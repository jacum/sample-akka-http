package io.dhlparcel.metrics

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import io.dhlparcel.StreamSpec
import org.scalamock.scalatest.MockFactory

import scala.collection.immutable

class MetricStreamSpec extends StreamSpec with MockFactory {
  private implicit val mat: ActorMaterializer = ActorMaterializer()

  private case class TestEvent(index: Long)

  "MetricStream" - {
    "collects metrics without reordering" in {

      val stubHttp = stub[HttpMetrics]
      (stubHttp.collectors _).when().returns(Set.empty)

      val stubMetrics = Metrics.default.copy(
        httpMetrics = stubHttp
      )

      MetricExtension(system).setMetrics(stubMetrics)

      val testFlow = MetricStream.metricFlow[TestEvent] { (event, metrics) =>
        val index = event.index
        metrics.httpMetrics.recordTotals(
          MetricLabel("GET"),
          MetricLabel(s"/test/$index")
        )
      }

      val testEvents = immutable.Seq(TestEvent(0L), TestEvent(1L))
      val testSource = Source.fromIterator(testEvents.iterator _)
      val testSink = TestSink.probe[TestEvent]

      val probe = testSource.via(testFlow).runWith(testSink)

      probe.ensureSubscription()
      probe.request(2)
      probe.expectNextN(testEvents)
      probe.expectComplete()

      inSequence {
        stubHttp.recordTotals _ verify(
          MetricLabel("GET"),
          MetricLabel("/test/0")
        )

        stubHttp.recordTotals _ verify(
          MetricLabel("GET"),
          MetricLabel("/test/1")
        )
      }
    }

    "handles exceptions in a metricFunc without losing elements" in {
      val testFlow = MetricStream.metricFlow[TestEvent] { (event, metrics) =>
        if(event.index == 1) throw new IllegalStateException("test exception - it is ok to see it in logs")
      }

      val testEvents = immutable.Seq(TestEvent(0L), TestEvent(1L), TestEvent(2L))
      val testSource = Source.fromIterator(testEvents.iterator _)
      val testSink = TestSink.probe[TestEvent]

      val probe = testSource.via(testFlow).runWith(testSink)

      probe.ensureSubscription()
      probe.request(3)
      probe.expectNextN(testEvents)
      probe.expectComplete()
    }
  }
}
