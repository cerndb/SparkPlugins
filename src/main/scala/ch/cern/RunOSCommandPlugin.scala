package ch.cern

import java.util.{Map => JMap}
import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import sys.process._

// Basic example of Spark Executor Plugin in Scala
// you can use this to execute a script
// Parameter: spark.cernSparkPlugin.command
class RunOSCommandPlugin extends SparkPlugin {

  val defaultCommand = "/usr/bin/touch /tmp/plugin.txt"
  // you can also use a script as command
  // --conf spark.cernSparkPlugin.command="./myscript.sh"
  // and use --files myscript.sh to distribute the script to the executors

  // Return the plugin's driver-side component.
  // No action, for this example
  override def driverPlugin(): DriverPlugin = null

  // Return the plugin's executor-side component.
  // This is an example plugin: run a configurable OS command at executor startup
  override def executorPlugin(): ExecutorPlugin = {
    new ExecutorPlugin() {

      override def init(myContext: PluginContext, extraConf: JMap[String, String]): Unit = {
        // Run the OS command, this is an example, customize and add error and stdout management as needed
        val command = myContext.conf.get("spark.cernSparkPlugin.command", defaultCommand)
        val process = Process(command).lineStream
        RunOSCommandPlugin.numSuccessfulPlugins += 1
      }

      override def shutdown(): Unit = {
        RunOSCommandPlugin.numSuccessfulTerminations += 1
      }
    }
  }

}

// Additional code to demonstrate the use of an associated object
// for example to implement helper value stores and registries.
object RunOSCommandPlugin {
  var numSuccessfulPlugins : Int = 0
  var numSuccessfulTerminations: Int = 0
}

