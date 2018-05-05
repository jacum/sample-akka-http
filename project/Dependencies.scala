import sbt._

object Dependencies {

  private val scalaVersion = "2.12.5"
  private val slf4jVersion = "1.7.25"

  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0"
  val typeSafeConfig = "com.typesafe" % "config" % "1.3.3"

  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" excludeAll(ExclusionRule("org.scala-lang", "scala-reflect"))

  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.5" % Test

  val scalaReflect = "org.scala-lang" % "scala-reflect" % scalaVersion
  val scalaCompiler = "org.scala-lang" % "scala-compiler" % scalaVersion
  val jclOverSlf4j = "org.slf4j" % "jcl-over-slf4j" % slf4jVersion

  object Prometheus {
    val prometheusVersion = "0.1.0"

    val common  = "io.prometheus" % "simpleclient_common"   % prometheusVersion
    val hotspot = "io.prometheus" % "simpleclient_hotspot"  % prometheusVersion
    val logback = "io.prometheus" % "simpleclient_logback"  % prometheusVersion

    val all = Seq(
      common, hotspot, logback
    )
  }

  object Akka {
    val akkaVersion = "2.5.11"
    val akkaHttpVersion = "10.1.1"

    val http          = "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion
    val httpCore      = "com.typesafe.akka" %% "akka-http-core"       % akkaHttpVersion
    val slf4j         = "com.typesafe.akka" %% "akka-slf4j"           % akkaVersion
    val stream        = "com.typesafe.akka" %% "akka-stream"          % akkaVersion
    val streamTestKit = "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion
    val httpTestKit   = "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion
    val circe = "de.heikoseeberger" %% "akka-http-circe" % "1.20.1"
    val all = Seq(
      http, slf4j, stream, circe, httpTestKit  % "test", streamTestKit % "test"
    )
  }

  object Other {
    val scalaJava8Compat  = "org.scala-lang.modules"        %%  "scala-java8-compat"  % "0.8.0"
    val logback           = "ch.qos.logback"                %   "logback-classic"     % "1.2.3"
    val logging           = "com.typesafe.scala-logging"    %%  "scala-logging"       % "3.9.0"
    val config            = "com.typesafe"                  %   "config"              % "1.3.2"
    val log4jOverSlf4j    = "org.slf4j"                     %   "log4j-over-slf4j"    % "1.7.25"

    val scalaTest         = "org.scalatest" %% "scalatest"                    % "3.0.4"
    val scalaMock         = "org.scalamock" %% "scalamock-scalatest-support"  % "3.6.0"

    val all = Seq(
      scalaJava8Compat, logback, logging, config, log4jOverSlf4j,
      scalaTest % "test", scalaMock % "test"
    )
  }

  val cassandraDriverCore = "com.datastax.cassandra" % "cassandra-driver-core" % "3.3.2"

  val dependencyOverrides = Seq(
    scalaReflect,
    scalaCompiler,
    typeSafeConfig,
    cassandraDriverCore
  )

  def excludeLog4j(moduleID: ModuleID) = moduleID.excludeAll(
    ExclusionRule("log4j", "log4j"),
    ExclusionRule("org.slf4j", "slf4j-log4j12"),
    ExclusionRule("commons-logging", "commons-logging"))

}
