package com.vtex.akkahttpseed.utils.aws

import akka.event.LoggingAdapter
import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler

import scala.util.Try

/**
  * AWS SDK is in Java 7 and use callback functions to return results.
  * AWSAsyncHandler is a custom handler in Scala that expose a Scala Future.
  *
  * Future is a Monad. Monad provides an easier computation flow with no need of complex errors handlers.
  *
  * @tparam T
  * @tparam W
  */
class AWSAsyncHandler[T <: AmazonWebServiceRequest, W] (implicit log: LoggingAdapter)
  extends AsyncHandler[T, W]
{

  private val promise = scala.concurrent.Promise[(T, W)]()
  val future = promise.future

  override def onError(exception: Exception): Unit = {
    log.error("Exception while calling AWS SDK: {}", exception)
    promise.failure(exception)
  }

  override def onSuccess(request: T, result: W): Unit = {
    promise.complete(Try {
      (request, result)
    })
  }

}