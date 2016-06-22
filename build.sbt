lazy val root = (project in file(".")).enablePlugins(JavaAppPackaging).enablePlugins(GitVersioning)

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"

name := "akka-http-docker-seed"

scalaVersion := "2.11.8"

resolvers += Resolver.sonatypeRepo("releases")

packageName in Docker := "sample-project/akka-http-docker-seed"
dockerExposedPorts := Seq(5000)

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.4.7"
  val scalaTestV = "2.2.6"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaV,
    "org.scalatest" %% "scalatest" % scalaTestV % "test",
    "com.github.coveo" % "uap-java" % "1.3.1-coveo1",
    "org.json4s" %% "json4s-jackson" % "3.3.0",
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "com.amazonaws" % "aws-java-sdk-sqs" % "1.11.9"
  )
}
unmanagedResourceDirectories in Compile += {
  baseDirectory.value / "src/main/resources"
}