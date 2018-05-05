package io.dhlparcel.metrics

import java.util.concurrent.atomic.AtomicReference

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId}
import io.dhlparcel.metrics.MetricExtensionImpl._

object MetricExtension extends ExtensionId[MetricExtensionImpl] {
  override def createExtension(system: ExtendedActorSystem): MetricExtensionImpl = new MetricExtensionImpl
}

class MetricExtensionImpl extends Extension {
  private val ref = new AtomicReference[MetricSettings](buildMetricSettings(Metrics.default))

  def setMetrics(metrics: Metrics): Unit = {
    ref.set(buildMetricSettings(metrics))
  }

  def metricSettings: MetricSettings = ref.get
}

object MetricExtensionImpl {
  private def buildMetricSettings(metrics: Metrics): MetricSettings = {
    val metricCollectors = new MetricCollectors
    metrics.collectors.foreach(metricCollectors.registry.register)
    val reporter = new MetricReporter(metricCollectors)
    MetricSettings(metricCollectors, metrics, reporter)
  }
}
