package ch.cern

import java.util.{Map => JMap}
import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

// Basic example of Spark Executor Plugin in Scala

class DemoPlugin extends SparkPlugin {

  /**
   * Return the plugin's driver-side component.
   *
   * @return The driver-side component, or set to null if one is not needed.
   */
  override def driverPlugin(): DriverPlugin = {
    new DriverPlugin() {
      override def init(sc: SparkContext, myContext: PluginContext): JMap[String, String] = {
        DemoPlugin.numSuccessfulPlugins += 1
        null
      }

      override def shutdown(): Unit = {
        DemoPlugin.numSuccessfulTerminations += 1
      }
    }
  }

  /**
   * Return the plugin's executor-side component.
   *
   * @return The executor-side component, or set to null if one is not needed.
   */
  override def executorPlugin(): ExecutorPlugin = {
    new ExecutorPlugin() {
      override def init(myContext: PluginContext, extraConf: JMap[String, String]): Unit = {
        DemoPlugin.numSuccessfulPlugins += 1
      }

      override def shutdown(): Unit = {
        DemoPlugin.numSuccessfulTerminations += 1
      }
    }
  }

}

object DemoPlugin {
  var numSuccessfulPlugins : Int = 0
  var numSuccessfulTerminations: Int = 0
}

