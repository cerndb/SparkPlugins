package ch.cern

import java.util.{Map => JMap}

import com.codahale.metrics.{Gauge, MetricRegistry}

import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

import org.apache.hadoop.fs.FileSystem.getGlobalStorageStatistics

/**
 * Instrument HDFS I/0, for Hadoop client version 2.8 or higher.
 * This is implemented using the new recommended method for Hadoop fs statistics gathering:
 * FileSystem.getGlobalStorageStatistics (only works with Hadoop 2.8 and higher).
 * Note: use with Spark compiled with Hadoop 3.2 profile or use with your own Hadoop client v 2.8 or higher.
 */

class HDFSMetrics32 extends SparkPlugin {

  // HDFS metrics registration using getGlobalStorageStatistics API (Hadoop 2.8 and higher)
  // Note, getGlobalStorageStatistics.get("fs_name") will return null till the first use of "fs_name"
  def hdfsMetrics(metricRegistry: MetricRegistry): Unit= {
    metricRegistry.register(MetricRegistry.name("hdfsBytesRead"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = getGlobalStorageStatistics.get("hdfs")
        hdfsStats match {
          case null => 0L
          case _ => hdfsStats.getLong("bytesRead")
        }

      }
    })

    metricRegistry.register(MetricRegistry.name("hdfsBytesWritten"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = getGlobalStorageStatistics.get("hdfs")
        hdfsStats match {
          case null => 0L
          case _ => hdfsStats.getLong("bytesWritten")
        }
      }
    })

    metricRegistry.register(MetricRegistry.name("hdfsReadOps"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = getGlobalStorageStatistics.get("hdfs")
        hdfsStats match {
          case null => 0L
          case _ => hdfsStats.getLong("readOps")
        }
      }
    })

    metricRegistry.register(MetricRegistry.name("hdfsWriteOps"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = getGlobalStorageStatistics.get("hdfs")
        hdfsStats match {
          case null => 0L
          case _ => hdfsStats.getLong("writeOps")
        }
      }
    })

    metricRegistry.register(MetricRegistry.name("hdfsLargeReadOps"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = getGlobalStorageStatistics.get("hdfs")
        hdfsStats match {
          case null => 0L
          case _ => hdfsStats.getLong("largeReadOps")
        }
      }
    })

    metricRegistry.register(MetricRegistry.name("hdfsBytesReadLocalHost"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = getGlobalStorageStatistics.get("hdfs")
        hdfsStats match {
          case null => 0L
          case _ => hdfsStats.getLong("bytesReadLocalHost")
        }
      }
    })

    metricRegistry.register(MetricRegistry.name("hdfsBytesReadDistanceOfOneOrTwo"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = getGlobalStorageStatistics.get("hdfs")
        hdfsStats match {
          case null => 0L
          case _ => hdfsStats.getLong("bytesReadDistanceOfOneOrTwo")
        }
      }
    })

    metricRegistry.register(MetricRegistry.name("hdfsBytesReadDistanceOfThreeOrFour"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = getGlobalStorageStatistics.get("hdfs")
        hdfsStats match {
          case null => 0L
          case _ => hdfsStats.getLong("bytesReadDistanceOfThreeOrFour")
        }
      }
    })

    metricRegistry.register(MetricRegistry.name("hdfsBytesReadDistanceOfFiveOrLarger"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = getGlobalStorageStatistics.get("hdfs")
        hdfsStats match {
          case null => 0L
          case _ => hdfsStats.getLong("bytesReadDistanceOfFiveOrLarger")
        }
      }
    })

    metricRegistry.register(MetricRegistry.name("hdfsBytesReadErasureCoded"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = getGlobalStorageStatistics.get("hdfs")
        hdfsStats match {
          case null => 0L
          case _ => hdfsStats.getLong("bytesReadErasureCoded")
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
        hdfsMetrics(myContext.metricRegistry)
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
        hdfsMetrics(myContext.metricRegistry)
      }
    }
  }

}

