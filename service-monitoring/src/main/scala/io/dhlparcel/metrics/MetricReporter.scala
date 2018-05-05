package io.dhlparcel.metrics

class MetricReporter(collectors: MetricCollectors) {
  def report: Report = Report(collectors)
}
