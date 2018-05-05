package io.dhlparcel.metrics

case class MetricSettings(
  metricCollectors: MetricCollectors,
  metrics: Metrics,
  reporter: MetricReporter
)
