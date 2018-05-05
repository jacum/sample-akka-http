import java.nio.charset.StandardCharsets

import sbt.io.Path._
import sbt.Keys.{name, sourceDirectory}
import Dependencies._
import scala.sys.process._

val date = ("date +%Y%m%d_%H%M%S" !!).trim()
val hash = ("git rev-parse --short HEAD" !!).trim()
val dockerTag = s"$date-$hash".trim()
val _ = IO.write(new File("version.txt"), dockerTag, StandardCharsets.UTF_8)

lazy val mainClassSetting = mainClass in Compile := Some("io.dhlparcel.sampleservice.Boot")

lazy val monitoring = (project in file("service-monitoring"))
  .settings(
    nonPublishSettings,
    codeQualitySettings,
    testSettings,
    scalaVersion := "2.12.4",
    libraryDependencies ++= Seq(
      Akka.stream,
      Akka.streamTestKit % "test",
      Other.scalaTest % "test",
      Other.scalaMock % "test"
    ) ++ Prometheus.all,
    name := "Service Monitoring",
    moduleName := "service-monitoring"
  )


lazy val main = (project in file("service-main"))
 .dependsOn(monitoring)
  .settings(
    commonSettings,
    nonPublishSettings,
    libraryDependencies ++=
      Akka.all ++ Prometheus.all ++ Other.all,
    name := "Service  Main",
    moduleName := "service-main"
)

lazy val sampleService = (project in file("."))
  .aggregate(main, monitoring)
  .dependsOn(main, monitoring)
  .enablePlugins(JavaServerAppPackaging, DockerSettingsPlugin)
  .settings(
    mainClassSetting,
    organization := "io.dhlparcel",
    name := "Example",
    normalizedName := "example-service",
    moduleName := "example-service",
    concurrentRestrictions in Global := Seq(Tags.limitAll(8))
  )

lazy val commonSettings = Seq(
  scalaVersion := "2.12.5",
  scalacOptions := Seq(
    "-encoding", "utf8",
    "-target:jvm-1.8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Xlog-reflective-calls",
    "-Ywarn-unused",
    "-Ywarn-unused-import"
  )
)

lazy val nonPublishSettings = Seq(
  publish := {}
)

lazy val testSettings = inConfig(Test)(Seq(
  testOptions := Seq(
    Tests.Argument(TestFrameworks.ScalaTest,
      "-oDS", "-u", "target/test-reports", "-h", "target/test-reports")
  ),
  fork := true,
  testForkedParallel := true,
  logBuffered := false
))


lazy val codeQualitySettings = Seq(
  scalacOptions ++= Seq(
    "-Ywarn-unused-import",
    "-Ypartial-unification"
  ),
  scalaBinaryVersion in ThisBuild := "2.12",
  scapegoatVersion in ThisBuild := "1.3.3",
  scapegoatDisabledInspections := Seq(
    "FinalModifierOnCaseClass"
  ),
  scapegoatIgnoredFiles := Seq(
    ".*/Boot.scala"
  )
)

lazy val apiDocumentationSettings = Seq(
  paradoxTheme := Some(builtinParadoxTheme("generic")),
  sourceDirectory in Paradox := baseDirectory.value / "doc",
  target in makeSite := target.value / "doc",

  mappings in Docker ++= {
    val srcDocDir = (target in makeSite).value
    val dstDocDir = (defaultLinuxInstallLocation in Docker).value
    srcDocDir.allPaths pair rebase(target.value, dstDocDir)
  }
)
