import java.nio.charset.StandardCharsets

import com.typesafe.sbt.SbtNativePackager.{Docker, Universal}
import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.packager.docker.ExecCmd
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport.{defaultLinuxInstallLocation, defaultLinuxLogsLocation, linuxPackageSymlinks}
import sbt.Keys.{javaOptions, normalizedName, version}
import sbt.{AutoPlugin, File, IO, Plugins, Setting}
import sbt.Keys.baseDirectory
import sbt._

object DockerSettingsPlugin extends AutoPlugin {
  private val JMXPort = 9013
  private val JMXExporterPort = 9101
  private val JMXExporterJar = "/opt/jmx_exporter/jmx_exporter.jar"
  private val JMXExporterConfig = "/opt/dhlparcel/conf/jmx_exporter-config.yml"
  override def requires: Plugins = JavaServerAppPackaging

  override def projectSettings: Seq[Setting[_]] = Seq(
    defaultLinuxLogsLocation := "/opt/dhlparcel/logs",
    defaultLinuxInstallLocation in Docker := "/opt/dhlparcel",
    linuxPackageSymlinks := Seq.empty,
    version in Docker := IO.read(new File("version.txt"), StandardCharsets.UTF_8),
    dockerBaseImage := "anapsix/alpine-java:latest",
    dockerRepository := Some("index.docker.io"),
    dockerUsername := Some("jacum"),
    dockerUpdateLatest := true,
    dockerExposedPorts := Seq(9000, JMXPort),
    //  dockerExposedVolumes := Seq(defaultLinuxLogsLocation.value),
//    dockerCommands := dockerCommands.value.filterNot {
//      case ExecCmd("RUN", args @ _*) => args.contains("chown")
//      case cmd                       => false
//    },
    dockerPackageMappings in Docker +=
        (baseDirectory.value / "docker" / "jmx_exporter-config.yml") -> "jmx_exporter-config.yml",
    dockerPackageMappings in Docker +=
        (baseDirectory.value / "docker" / "jmx_exporter.jar") -> "jmx_exporter.jar",

    dockerCommands ++= Seq(
      ExecCmd("ADD", "jmx_exporter-config.yml", JMXExporterConfig),
      ExecCmd("ADD", "jmx_exporter.jar", JMXExporterJar),
      ExecCmd("RUN", "mkdir", "-p", s"${(defaultLinuxLogsLocation in Docker).value}/${normalizedName.value}")
    ),

    javaOptions in Universal ++= Seq(
      "-Djava.net.preferIPv4Stack=true",
      "-Djava.awt.headless=true",
      "-Dcom.sun.management.jmxremote",
      s"-Dcom.sun.management.jmxremote.port=$JMXPort",
      s"-Dcom.sun.management.jmxremote.rmi.port=$JMXPort",
      "-Dcom.sun.management.jmxremote.ssl=false",
      "-Dcom.sun.management.jmxremote.authenticate=false",
      "-Dcom.sun.management.jmxremote.local.only=false",
      s"-J-javaagent:$JMXExporterJar=$JMXExporterPort:$JMXExporterConfig",
      "-J-XX:-HeapDumpOnOutOfMemoryError",
      s"-J-XX:HeapDumpPath=${defaultLinuxLogsLocation.value}/${normalizedName.value}",
      "-J-XX:+UseConcMarkSweepGC",
      "-J-XX:CMSInitiatingOccupancyFraction=60",
      "-J-XX:+UseCMSInitiatingOccupancyOnly",
      "-J-Xms256M",
      "-J-Xmx256M",
      s"-J-Xloggc:${defaultLinuxLogsLocation.value}/${normalizedName.value}/gc.log",
      "-J-XX:+UseGCLogFileRotation",
      "-J-XX:NumberOfGCLogFiles=4",
      "-J-XX:GCLogFileSize=256M",
      "-J-XX:+PrintGC",
      "-J-XX:+PrintGCDateStamps",
      "-J-XX:+PrintTenuringDistribution",
      "-J-XX:+PrintGCApplicationStoppedTime",
      "-J-XX:+PrintGCApplicationConcurrentTime",
      "-J-XX:+PrintGCDetails",
      "-J-XX:+PerfDisableSharedMem"
    )
  )
}
