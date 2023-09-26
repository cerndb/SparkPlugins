package ch.cern

import io.pyroscope.javaagent.PyroscopeAgent
import io.pyroscope.javaagent.EventType
import io.pyroscope.javaagent.config.Config
import io.pyroscope.http.Format

import java.net.InetAddress
import java.util.{Map => JMap}
import scala.collection.JavaConverters._

import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

/**
 * Use this Plugin to configure Pyroscope java agent data collection on Spark executors
 * See https://grafana.com/docs/pyroscope/latest/configure-client/language-sdks/java/
 *
 * This plugin adds the following configurations:
 *   --conf spark.pyroscope.server - > default "http://localhost:4040", update to match the server name and port used by Pyroscope
 *   --conf spark.pyroscope.applicationName -> default spark.conf.get("spark.app.id")
 *   --conf spark.pyroscope.eventType -> default ITIMER, possible values ITIMER, CPU, WALL, ALLOC, LOCK
*/
class PyroscopePlugin extends SparkPlugin {

  // Return the plugin's driver-side component.
  override def driverPlugin(): DriverPlugin = {
    new DriverPlugin() {
      override def init(sc: SparkContext, myContext: PluginContext): JMap[String, String] = {
        Map.empty[String, String].asJava
      }
    }
  }

  // Return the plugin's executor-side component.
  // This implements an executor plugin to set up the configuration for Pyroscope
  override def executorPlugin(): ExecutorPlugin = {
    new ExecutorPlugin() {
      override def init(myContext: PluginContext, extraConf: JMap[String, String]): Unit = {

        // Pyroscope server URL, match the URL with your Pyroscope runtime
        val pyroscopeServerUrl = myContext.conf.get("spark.pyroscope.server", "http://localhost:4040")

        // this will be used for the application name
        // note, in local mode spark.app.id in null, we use "local" to handle the case
        val pyroscopeApplicationName = myContext.conf.get("spark.pyroscope.applicationName",
          myContext.conf.get("spark.app.id", "local"))

        val executorId = myContext.executorID
        val localHostname = InetAddress.getLocalHost.getHostName

        // this sets the event type to profile, default ITIMER, possible values ITIMER, CPU, WALL, ALLOC, LOCK
        val pyroscopeEventType = myContext.conf.get("spark.pyroscope.eventType", "ITIMER")

        val eventType = pyroscopeEventType.toUpperCase match {
          case "ITIMER" => EventType.ITIMER
          case "CPU" => EventType.CPU
          case "WALL" => EventType.WALL
          case "ALLOC" => EventType.ALLOC
          case "LOCK" => EventType.LOCK
          case _ => throw new IllegalArgumentException(s"Invalid event type: $pyroscopeEventType")
        }

        PyroscopeAgent.start(
          new Config.Builder()
            .setApplicationName(pyroscopeApplicationName)
            .setProfilingEvent(eventType)
            .setFormat(Format.JFR)
            .setServerAddress(pyroscopeServerUrl)
            .setLabels(Map("executorId" -> executorId.toString, "hostname" -> localHostname).asJava)
            .build()
        )
      }
    }
  }

}
