package io.dhlparcel.metrics

case class MetricLabel(name: String) extends AnyVal {
  override def toString: String = name
}

