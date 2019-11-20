package ch.cern

import java.util.{Map => JMap}
import scala.collection.JavaConverters._

import com.codahale.metrics.{Gauge, MetricRegistry}

import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

import org.apache.hadoop.fs.FileSystem

// Instrument S3A I/0, for Hadoop client version 2.7
class S3AMetrics27 extends SparkPlugin {

  // S3A metrics registration using Hadoop 2.7 API
  def s3aMetrics(metricRegistry: MetricRegistry): Unit= {

    metricRegistry.register(MetricRegistry.name("s3aBytesRead"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals("s3a"))
        hdfsStats.map(_.getBytesRead).getOrElse(0L)
      }
    })

    metricRegistry.register(MetricRegistry.name("s3aBytesWritten"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals("s3a"))
        hdfsStats.map(_.getBytesWritten).getOrElse(0L)
      }
    })

    metricRegistry.register(MetricRegistry.name("s3aReadOps"), new Gauge[Int] {
      override def getValue: Int = {
        val hdfsStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals("s3a"))
        hdfsStats.map(_.getReadOps).getOrElse(0)
      }
    })

    metricRegistry.register(MetricRegistry.name("s3aWriteOps"), new Gauge[Int] {
      override def getValue: Int = {
        val hdfsStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals("s3a"))
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
        s3aMetrics(myContext.metricRegistry)
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
          s3aMetrics(myContext.metricRegistry)
        }
      }
    }
  }

}

