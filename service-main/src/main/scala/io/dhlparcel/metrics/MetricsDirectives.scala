package io.dhlparcel.metrics

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

class MetricsDirectives(
  httpMetrics: HttpMetrics
) {

  def collectMetrics: Directive0 =
    countRequests &
      collectTiming


  private def countRequests: Directive0 = {
    (extractRequestMethodAsLabel & withPathLabels).tmap {
      case (req, resource) => httpMetrics.recordTotals(req, resource)
    }
  }

  private def collectTiming =
    (extractRequestMethodAsLabel & withPathLabels).tflatMap {
      case (req, resource) =>
        httpMetrics.recordLatency(req, resource) { time =>
          mapResponse { res =>
            time(res.status.intValue)
            res
          }}
    }

  private val docsLabel         = pathLabel("api" / "sample" / "docs", MetricLabel("docs"))
  private val healthCheckLabel  = pathLabel("health" / "check",       MetricLabel("health"))
  private val metricsLabel      = pathLabel("metrics",                MetricLabel("metrics"))
  private val default           = provide(MetricLabel("other"))

  /** Request context extraction forces evaluation of route for each request.
    * This allows for counting of requests.
    */
  private val extractRequestMethodAsLabel: Directive1[MetricLabel] = {
    extractRequest.map { req =>
      MetricLabel(req.method.name())
    }
  }

  /**
    * Helper method to label simple paths.
    * Checks an unmatched part of a request path thus it must be applied to a route before path* directives
    */
  private def pathLabel(pm: PathMatcher[_], resourceLabel: MetricLabel): Directive1[MetricLabel] = {
    pathPrefixTest(pm).tmap { _ => resourceLabel }
  }
  /**
    * Extract labels from paths using akka path matcher directives
    */
  private val withPathLabels: Directive1[MetricLabel] = {
    docsLabel | healthCheckLabel | metricsLabel | default
  }

}


