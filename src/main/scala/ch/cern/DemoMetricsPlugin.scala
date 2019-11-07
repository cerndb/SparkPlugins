package ch.cern

import java.util.{Map => JMap}

import com.codahale.metrics.{Gauge, MetricRegistry}
import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

class DemoMetricsPlugin extends SparkPlugin {

  /**
   * Return the plugin's driver-side component.
   *
   * @return The driver-side component, or null if one is not needed.
   */
  override def driverPlugin(): DriverPlugin = {
    new DriverPlugin() {
      override def init(sc: SparkContext, myContext: PluginContext): JMap[String, String] = {
        val metricRegistry = myContext.metricRegistry
        // Gauge for testing
        metricRegistry.register(MetricRegistry.name("DriverTest42"), new Gauge[Int] {
          override def getValue: Int = 42
        })
        null
      }
    }
  }

    /**
   * Return the plugin's executor-side component.
   *
   * @return The executor-side component, or null if one is not needed.
   */
  override def executorPlugin(): ExecutorPlugin = {
    new ExecutorPlugin {
      override def init(myContext:PluginContext, extraConf:JMap[String, String])  = {
        val metricRegistry = myContext.metricRegistry
        // Gauge for testing
        metricRegistry.register(MetricRegistry.name("ExecutorTest42"), new Gauge[Int] {
          override def getValue: Int = 42
        })
      }
    }
  }

}

