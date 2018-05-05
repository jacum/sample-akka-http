package io.dhlparcel

import akka.actor.ActorSystem
import akka.testkit._
import org.scalatest.{BeforeAndAfterAll, FreeSpec, GivenWhenThen, MustMatchers}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContextExecutor


abstract class StreamSpec
  extends FreeSpec with GivenWhenThen with MustMatchers
  with TestKitBase
  with BeforeAndAfterAll
  with ScalaFutures {

  /** Lazily initialized to get access to concrete spec class name */
  implicit lazy val system: ActorSystem = ActorSystem(getClass.getSimpleName)
  implicit lazy val executionContext: ExecutionContextExecutor = system.dispatcher

  override def beforeAll(): Unit = {
    super.beforeAll()
    /*start*/ system
  }

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

}
