package com.vtex.akkahttpseed.utils.aws.sqs

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler

import scala.util.Try

/**
  * aws sdk in java use callback functions to return results, AWSAsyncHandler are handlers in scala that
  * expose scala futures for a more linear coding without the need of isolated callbacks
  *
  * @tparam T
  * @tparam W
  */
class AWSAsyncHandler[T <: AmazonWebServiceRequest, W] extends AsyncHandler[T, W] {

  private val promise = scala.concurrent.Promise[(T, W)]()
  val future = promise.future

  override def onError(exception: Exception): Unit = {
    promise.failure(exception)
  }

  override def onSuccess(request: T, result: W): Unit = {
    promise.complete(Try {
      (request, result)
    })
  }

}