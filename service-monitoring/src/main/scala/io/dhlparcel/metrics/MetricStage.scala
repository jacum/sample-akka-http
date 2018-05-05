package io.dhlparcel.metrics

import akka.stream._
import akka.stream.stage._

import scala.util.control.NonFatal

class MetricStage[T](metricFunc: (T, Metrics) => Unit) extends GraphStage[FlowShape[T, T]]{
  val in: Inlet[T] = Inlet[T]("Metric.in")
  val out: Outlet[T] = Outlet[T]("Metric.out")

  override def createLogic(inheritedAttributes: Attributes): MetricStageLogic[T] = {
    new MetricStageLogic(in, out, shape, metricFunc)
  }

  override def shape: FlowShape[T, T] = FlowShape.of(in, out)
}

class MetricStageLogic[T](in: Inlet[T], out: Outlet[T], shape: Shape, metricFunc: (T, Metrics) => Unit)
  extends GraphStageLogic(shape) with InHandler with OutHandler with StageLogging
{
  import MetricStageLogic._
  private var metricSettings: MetricSettings = _

  override def preStart(): Unit = {
    val actorMat = materializerDowncast(materializer)
    metricSettings = MetricExtension(actorMat.system).metricSettings
  }

  override def onPush(): Unit = {
    val grabbedValue = grab(in)
    // The stage must not fail if something wrong happened in metricFunc
    try {
      metricFunc(grabbedValue, metricSettings.metrics)
    } catch {
      case NonFatal(e) =>
        val msg = e.getMessage
        val errorClass = e.getClass.getSimpleName
        log.error(e, s"metric error: $errorClass($msg)")
    }
    push(out, grabbedValue)
  }

  override def onPull(): Unit = {
    pull(in)
  }

  setHandler(in,  this)
  setHandler(out, this)
}

object MetricStageLogic {
  def materializerDowncast(materializer: Materializer): ActorMaterializer = {
    //See akka.stream.ActorMaterializerHelper.downcast
    materializer match {
      case m: ActorMaterializer => m
      case _ => throw new IllegalArgumentException(s"required [${classOf[ActorMaterializer].getName}] " +
        s"but got [${materializer.getClass.getName}]")
    }
  }
}

