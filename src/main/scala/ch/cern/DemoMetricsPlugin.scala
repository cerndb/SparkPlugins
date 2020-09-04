package ch.cern

import java.util.{Map => JMap}
import scala.collection.JavaConverters._

import com.codahale.metrics.{Gauge, MetricRegistry}
import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

class DemoMetricsPlugin extends SparkPlugin {

  // Return the plugin's driver-side component.
  override def driverPlugin(): DriverPlugin = {
    new DriverPlugin() {
      override def init(sc: SparkContext, myContext: PluginContext): JMap[String, String] = {
        val metricRegistry = myContext.metricRegistry
        // Gauge for testing
        metricRegistry.register(MetricRegistry.name("DriverTest42"), new Gauge[Int] {
          override def getValue: Int = 42
        })
        Map.empty[String, String].asJava
      }
    }
  }

  // Return the plugin's executor-side component.
  override def executorPlugin(): ExecutorPlugin = {
    new ExecutorPlugin {
      override def init(myContext:PluginContext, extraConf:JMap[String, String])  = {
        // Gauge for testing
        val metricRegistry = myContext.metricRegistry
        metricRegistry.register(MetricRegistry.name("ExecutorTest42"), new Gauge[Int] {
          override def getValue: Int = 42
          })
      }
    }
  }

}
