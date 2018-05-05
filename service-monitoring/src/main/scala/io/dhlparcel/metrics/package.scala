package io.dhlparcel

import java.time.Duration

package object metrics {
  private val NanosInSec = 1000000000.0
  implicit class RichDuration(duration: Duration) {
    /**
      * Returns the duration in seconds with nanoseconds as a fraction of a second.
      * <p>
      *   ```
      *     val startTime = Instant.parse("2017-11-10T00:00:00.000Z")
      *     val endTime   = Instant.parse("2017-11-10T01:00:01.001001001Z")
      *     val secsWithNanos = Duration.between(startTime, endTime).inSecondsWithNanos
      *     // secsWithNanos = 3601.001001001
      *   ```
      * </p>
      */
    def inSecondsWithNanos: Double = {
      val seconds = duration.getSeconds.toDouble
      val nanos = duration.getNano.toDouble
      val nanosInSecs = nanos / NanosInSec
      seconds + nanosInSecs
    }
  }
}
