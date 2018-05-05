package io.dhlparcel.docs

import java.nio.file.Path
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directive.{addByNameNullaryApply, addDirectiveApply}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

/**
  * Defines a route to API documentation
  * @param rootDirectoryPath root directory of documentation.
  *                          It must contain ''index.html''
  */
class DocResources(rootDirectoryPath: Path) {

  private def index(uri: Uri) = {
    val endWith = if (uri.path.endsWithSlash) uri.path + _ else uri.path / _
    uri.withPath(endWith("index.html"))
  }

  private def redirectToIndex =
    pathEndOrSingleSlash {
      extractUri { uri =>
        redirect(index(uri), PermanentRedirect)
      }
    }

  def route: Route =
    pathPrefix("api" / "sample" / "docs") {
      redirectToIndex ~
        getFromDirectory(rootDirectoryPath.toString) ~
        complete(NotFound -> "There are no API docs :(. Please contact support regarding this issue")
    }
}
