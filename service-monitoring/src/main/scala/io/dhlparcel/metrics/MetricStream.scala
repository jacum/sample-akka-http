package io.dhlparcel.metrics

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink}

object MetricStream {
  def metricFlow[T](metricFunc: (T, Metrics) => Unit): Flow[T, T, NotUsed] = {
    Flow[T].via(new MetricStage[T](metricFunc))
  }

  def metricSink[T](metricFunc: (T, Metrics) => Unit): Sink[T, NotUsed] = {
    val flow = metricFlow(metricFunc)
    flow.to(Sink.ignore)
  }
}
