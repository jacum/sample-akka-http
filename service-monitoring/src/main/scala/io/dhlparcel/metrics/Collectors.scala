package io.dhlparcel.metrics

import io.prometheus.client.Collector

trait Collectors {
  def collectors: Set[Collector]
}
