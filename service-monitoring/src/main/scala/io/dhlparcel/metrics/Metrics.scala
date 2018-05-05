package io.dhlparcel.metrics

import io.dhlparcel.metrics
import io.prometheus.client.Collector
import io.dhlparcel.metrics.prometheus._

case class Metrics(
  httpMetrics: HttpMetrics
) extends Collectors {
  override def collectors: Set[Collector] = {
      httpMetrics.collectors
  }
}

object Metrics {
  def default: Metrics = metrics.Metrics(
    httpMetrics         = new PrometheusHttpMetrics()
  )
}