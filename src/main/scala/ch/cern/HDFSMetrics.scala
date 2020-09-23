package ch.cern

import java.util.{Map => JMap}
import scala.collection.JavaConverters._

import com.codahale.metrics.{Gauge, MetricRegistry}

import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

import org.apache.hadoop.fs.FileSystem.getGlobalStorageStatistics

/**
 * Monitor HDFS I/0 metrics using Hadoop's API FileSystem.getGlobalStorageStatistics
 * FileSystem.getGlobalStorageStatistics has been introduced in Hadoop (client) version 2.8
 *
 * Note: use with Spark 3.x built with Hadoop 3.2 profile (or higher, when available)
 * or use it with Spark built without Hadoop and use your own Hadoop client version 2.8 or higher.
 */
class HDFSMetrics extends SparkPlugin {

  val fsName = "hdfs"
  val fsMetrics = Seq("bytesRead", "bytesWritten", "readOps", "writeOps", "largeReadOps",
    "bytesReadLocalHost", "bytesReadDistanceOfOneOrTwo", "bytesReadDistanceOfThreeOrFour",
    "bytesReadDistanceOfFiveOrLarger", "bytesReadErasureCoded")

  // Note: getGlobalStorageStatistics.get("fsName") will return null till the first use of "fsName"
  // This registers the metrics and their getValue method
  def hdfsMetrics(myContext: PluginContext): Unit= {
    val metricRegistry = myContext.metricRegistry
    fsMetrics.foreach ( name =>
      metricRegistry.register(MetricRegistry.name(name), new Gauge[Long] {
        override def getValue: Long = {
          val fsStats = getGlobalStorageStatistics.get(fsName)
          fsStats match {
            case null => 0L
            case _ => fsStats.getLong(name)
          }
        }
      })
    )
  }

  // Return the plugin's driver-side component.
  // register metrics conditional to --conf spark.cernSparkPlugin.registerOnDriver=true
  override def driverPlugin(): DriverPlugin = {
    new DriverPlugin() {
      override def init(sc: SparkContext, myContext: PluginContext): JMap[String, String] = {
        val registerOnDriver =
          myContext.conf.getBoolean("spark.cernSparkPlugin.registerOnDriver", true)
        if (registerOnDriver) {
          hdfsMetrics(myContext)
        }
        Map.empty[String, String].asJava
      }
    }
  }

   // Return the plugin's executor-side component.
  override def executorPlugin(): ExecutorPlugin = {
    new ExecutorPlugin() {
      override def init(myContext:PluginContext, extraConf:JMap[String, String])  = {
        hdfsMetrics(myContext)
      }
    }
  }

}

