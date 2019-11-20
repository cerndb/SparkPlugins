package ch.cern

import java.util.{Map => JMap}
import scala.collection.JavaConverters._

import com.codahale.metrics.{Gauge, MetricRegistry}

import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

import org.apache.hadoop.fs.FileSystem

// Instrument Hadoop-XRootD connector I/0 for CERn EOS storage, use with Hadoop client version 2.7 and higher
class ROOTMetrics27 extends SparkPlugin {

  // ROOT metrics registration using Hadoop 2.7 API
  def rootMetrics(metricRegistry: MetricRegistry): Unit= {

    metricRegistry.register(MetricRegistry.name("rootBytesRead"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals("root"))
        hdfsStats.map(_.getBytesRead).getOrElse(0L)
      }
    })

    metricRegistry.register(MetricRegistry.name("rootBytesWritten"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals("root"))
        hdfsStats.map(_.getBytesWritten).getOrElse(0L)
      }
    })

    metricRegistry.register(MetricRegistry.name("rootReadOps"), new Gauge[Int] {
      override def getValue: Int = {
        val hdfsStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals("root"))
        hdfsStats.map(_.getReadOps).getOrElse(0)
      }
    })

    metricRegistry.register(MetricRegistry.name("rootWriteOps"), new Gauge[Int] {
      override def getValue: Int = {
        val hdfsStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals("root"))
        hdfsStats.map(_.getWriteOps).getOrElse(0)
      }
    })

  }

  /**
   * Return the plugin's driver-side component.
   *
   * @return The driver-side component, or null if one is not needed.
   */
  override def driverPlugin(): DriverPlugin = {
    new DriverPlugin() {
      override def init(sc: SparkContext, myContext: PluginContext): JMap[String, String] = {
        rootMetrics(myContext.metricRegistry)
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
        // Don't register executor plugin if in local mode
        if (! myContext.conf.get("spark.master").startsWith("local")) {
          rootMetrics(myContext.metricRegistry)
        }
      }
    }
  }

}

