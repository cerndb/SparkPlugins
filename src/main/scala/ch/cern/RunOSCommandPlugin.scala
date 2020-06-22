package ch.cern

import java.util.{Map => JMap}
import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import sys.process._

// Basic example of Spark Executor Plugin in Scala

class RunOSCommandPlugin extends SparkPlugin {

  /**
   * Return the plugin's driver-side component.
   *
   * @return The driver-side component, or set to null if one is not needed.
   */
  override def driverPlugin(): DriverPlugin =null

  /**
   * Return the plugin's executor-side component.
   * Run an OS command at executor startup
   *
   * @return The executor-side component, or set to null if one is not needed.
   */
  override def executorPlugin(): ExecutorPlugin = {
    new ExecutorPlugin() {
      val command = "/usr/bin/touch /tmp/plugin.txt"
      // val command = "./myscript.sh" // use --files myscript.sh to distribute to the executors
      override def init(myContext: PluginContext, extraConf: JMap[String, String]): Unit = {
        // Run the OS command, this is an example, customize and add error and stdout management as needed
        val process = Process(command).lineStream
        DemoPlugin.numSuccessfulPlugins += 1
      }

      override def shutdown(): Unit = {
        RunOSCommandPlugin.numSuccessfulTerminations += 1
      }
    }
  }

}

object RunOSCommandPlugin {
  var numSuccessfulPlugins : Int = 0
  var numSuccessfulTerminations: Int = 0
}

