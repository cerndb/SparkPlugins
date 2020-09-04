package ch.cern

import java.util.{Map => JMap}
import scala.collection.JavaConverters._

import com.codahale.metrics.{Gauge, MetricRegistry}

import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

import org.apache.hadoop.fs.FileSystem

/**
 * Instrument Cloud FS I/0 using Hadoop 2.7 client API for FileSystem.getAllStatistics
 * Note this API is deprecated in more recent versions of Hadoop.
 * Requires the name of the Hadoop compatible filesystem to measure (s3a, oci, gs, root, etc).
 * Limitation: currently supports only one filesystem at a time
 *
 * Parameters:
 * filesystem name:
 *   --conf spark.cernSparkPlugin.cloudFsName=<name of the filesystem> (example: s3a)
 * register metrics on the driver conditional to
 *   --conf spark.cernSparkPlugin.registerOnDriver=true
 *
 */
class CloudFSMetrics27 extends SparkPlugin {

  def s3aMetrics(myContext: PluginContext): Unit= {

    val fsName = myContext.conf.get("spark.cernSparkPlugin.cloudFsName")
    val metricRegistry = myContext.metricRegistry

    metricRegistry.register(MetricRegistry.name("bytesRead"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals(fsName))
        hdfsStats.map(_.getBytesRead).getOrElse(0L)
      }
    })

    metricRegistry.register(MetricRegistry.name("bytesWritten"), new Gauge[Long] {
      override def getValue: Long = {
        val hdfsStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals(fsName))
        hdfsStats.map(_.getBytesWritten).getOrElse(0L)
      }
    })

    metricRegistry.register(MetricRegistry.name("readOps"), new Gauge[Int] {
      override def getValue: Int = {
        val hdfsStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals(fsName))
        hdfsStats.map(_.getReadOps).getOrElse(0)
      }
    })

    metricRegistry.register(MetricRegistry.name("writeOps"), new Gauge[Int] {
      override def getValue: Int = {
        val hdfsStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals(fsName))
        hdfsStats.map(_.getWriteOps).getOrElse(0)
      }
    })

  }

  // Return the plugin's driver-side component.
  // register metrics conditional to --conf spark.cernSparkPlugin.registerOnDriver=true
  override def driverPlugin(): DriverPlugin = {
    new DriverPlugin() {
      override def init(sc: SparkContext, myContext: PluginContext): JMap[String, String] = {
        val registerOnDriver =
          myContext.conf.getBoolean("spark.cernSparkPlugin.registerOnDriver", false)
        if (registerOnDriver) {
          s3aMetrics(myContext)
        }
        Map.empty[String, String].asJava
      }
    }
  }

   // Return the plugin's executor-side component.
  override def executorPlugin(): ExecutorPlugin = {
    new ExecutorPlugin {
      override def init(myContext:PluginContext, extraConf:JMap[String, String])  = {
          s3aMetrics(myContext)
      }
    }
  }

}

