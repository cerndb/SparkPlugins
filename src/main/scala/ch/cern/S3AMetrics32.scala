package ch.cern

import java.util.{Map => JMap}

import com.codahale.metrics.{Gauge, MetricRegistry}

import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

import org.apache.hadoop.fs.FileSystem.getGlobalStorageStatistics

/**
 * Instrument S3A I/0, for Hadoop client version 2.8 or higher.
 * This is implemented using the new recommended method for Hadoop fs statistics gathering:
 * FileSystem.getGlobalStorageStatistics (only works with Hadoop 2.8 and higher).
 * Note: use with Spark compiled with Hadoop 3.2 profile or use with your own Hadoop client v 2.8 or higher.
 */

class S3AMetrics32 extends SparkPlugin {

  // S3A metrics registration using getGlobalStorageStatistics API (Hadoop 2.8 and higher)
  // Note, getGlobalStorageStatistics.get("fs_name") will return null till the first use of "fs_name"
  def s3aMetrics(metricRegistry: MetricRegistry): Unit= {
    metricRegistry.register(MetricRegistry.name("s3aBytesRead"), new Gauge[Long] {
      override def getValue: Long = {
        val s3aStats = getGlobalStorageStatistics.get("s3a")
        s3aStats match {
          case null => 0L
          case _ => s3aStats.getLong("bytesRead")
        }

      }
    })

    metricRegistry.register(MetricRegistry.name("s3aBytesWritten"), new Gauge[Long] {
      override def getValue: Long = {
        val s3aStats = getGlobalStorageStatistics.get("s3a")
        s3aStats match {
          case null => 0L
          case _ => s3aStats.getLong("bytesWritten")
        }
      }
    })

    metricRegistry.register(MetricRegistry.name("s3aReadOps"), new Gauge[Long] {
      override def getValue: Long = {
        val s3aStats = getGlobalStorageStatistics.get("s3a")
        s3aStats match {
          case null => 0L
          case _ => s3aStats.getLong("readOps")
        }
      }
    })

    metricRegistry.register(MetricRegistry.name("s3aWriteOps"), new Gauge[Long] {
      override def getValue: Long = {
        val s3aStats = getGlobalStorageStatistics.get("s3a")
        s3aStats match {
          case null => 0L
          case _ => s3aStats.getLong("writeOps")
        }
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
        s3aMetrics(myContext.metricRegistry)
      }
    }
  }

}

