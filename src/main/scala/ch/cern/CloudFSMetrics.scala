package ch.cern

import java.util.{Map => JMap}
import scala.collection.JavaConverters._

import com.codahale.metrics.{Gauge, MetricRegistry}

import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

import org.apache.hadoop.fs.FileSystem.getGlobalStorageStatistics

/**
 * Monitor S3A I/0 metrics using Hadoop's API FileSystem.getGlobalStorageStatistics
 * FileSystem.getGlobalStorageStatistics has been introduced in Hadoop (client) version 2.8
 *
 * Note: use with Spark 3.x built with Hadoop 3.2 profile (or higher, when available)
 * or use it with Spark built without Hadoop and use your own Hadoop client version 2.8 or higher.
 * Limitation: this currently supports only one filesystem at a time
 *
 * Parameters:
 * filesystem name:
 *   --conf spark.cernSparkPlugin.cloudFsName=<name of the filesystem> (example: s3a)
 * register metrics on the driver conditional to
 *   --conf spark.cernSparkPlugin.registerOnDriver=true
 *
 */
class CloudFSMetrics extends SparkPlugin {

  val fsMetrics = Seq("bytesRead", "bytesWritten", "readOps", "writeOps")

  // This registers the metrics and their getValue method
  // Note: getGlobalStorageStatistics.get("fsName") will return null till the first use of "fsName"
  def hdfsMetrics(myContext: PluginContext): Unit= {
    val fsName = myContext.conf.get("spark.cernSparkPlugin.cloudFsName")
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
          myContext.conf.getBoolean("spark.cernSparkPlugin.registerOnDriver", false)
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

