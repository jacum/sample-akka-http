package io.dhlparcel.metrics.prometheus

import io.dhlparcel.metrics.{HttpMetrics, MetricLabel}
import io.prometheus.client.Collector
import io.prometheus.client.Counter
import io.prometheus.client.Histogram

class PrometheusHttpMetrics extends HttpMetrics {
  private val requestsTotal =
    Counter
      .build()
      .name("http_requests_total")
      .help("Total number of received HTTP requests")
      .labelNames("method", "resource")
      .create()

  private val requestsLatency: Histogram = {
    val buckets =
      for (
        p <- Vector[Double](0.0001, 0.001, 0.01, 0.1, 1, 10);
        s <- Vector(1, 2, 5)
      ) yield p * s

      Histogram
        .build()
        .buckets(buckets: _*)
        .name("http_requests_latency_seconds")
        .help("Latency of HTTP requests processing in seconds")
        .labelNames("method", "resource")
        .create()
  }

  override def recordTotals(
    method: MetricLabel,
    resource: MetricLabel
  ): Unit = {
    val labelNames = Seq(method.name, resource.name)
    requestsTotal.labels(labelNames:_*).inc()
  }
  override def recordLatency[T](
    method: MetricLabel,
    resource: MetricLabel
  )(
    complete: (Int => Unit) => T
  ): T = {
    val labelNames = Seq(method.name, resource.name)
    val timer = requestsLatency.labels(labelNames:_*).startTimer
    complete((_: Int) => timer.observeDuration())
  }

  override def collectors: Set[Collector] = Set(requestsTotal, requestsLatency)
}
