package io.dhlparcel.metrics

trait HttpMetrics extends Collectors {
  def recordTotals(
    method: MetricLabel,
    resource: MetricLabel): Unit
  def recordLatency[T](
    method: MetricLabel,
    resource: MetricLabel
  )(complete: (Int => Unit) => T): T
}
