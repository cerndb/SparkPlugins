package ch.cern

import java.util.{Map => JMap}
import scala.collection.JavaConverters._

import com.codahale.metrics.{Gauge, MetricRegistry}

import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

import org.apache.hadoop.fs.FileSystem

import org.slf4j.LoggerFactory

/**
 * Instrument Cloud FS I/0 using Hadoop 2.7 client API for FileSystem.getAllStatistics
 * Note this API is deprecated in more recent versions of Hadoop.
 * Requires the name of the Hadoop compatible filesystem to measure (s3a, oci, gs, root, etc).
 * Limitation: currently supports only one filesystem at a time
 *
 * Parameters:
 * filesystem name:
 *   --conf spark.cernSparkPlugin.cloudFsName=<name of the Hadoop compatible filesystem> (example: s3a)
 * register metrics on the driver conditional to
 *   --conf spark.cernSparkPlugin.registerOnDriver=true
 *
 */
class CloudFSMetrics27 extends SparkPlugin {

  lazy val logger = LoggerFactory.getLogger(this.getClass.getName)

  def cloudFilesystemMetrics(myContext: PluginContext): Unit= {

    val fsName = myContext.conf.getOption("spark.cernSparkPlugin.cloudFsName")
    if (fsName.isEmpty) {
      logger.error("spark.cernSparkPlugin.cloudFsName needs to be set when using the ch.cern.CloudFSMetrics Plugin.")
      throw new IllegalArgumentException
    }
    val metricRegistry = myContext.metricRegistry

    metricRegistry.register(MetricRegistry.name("bytesRead"), new Gauge[Long] {
      override def getValue: Long = {
        val cloudFSStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals(fsName))
        cloudFSStats.map(_.getBytesRead).getOrElse(0L)
      }
    })

    metricRegistry.register(MetricRegistry.name("bytesWritten"), new Gauge[Long] {
      override def getValue: Long = {
        val cloudFSStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals(fsName))
        cloudFSStats.map(_.getBytesWritten).getOrElse(0L)
      }
    })

    metricRegistry.register(MetricRegistry.name("readOps"), new Gauge[Int] {
      override def getValue: Int = {
        val cloudFSStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals(fsName))
        cloudFSStats.map(_.getReadOps).getOrElse(0)
      }
    })

    metricRegistry.register(MetricRegistry.name("writeOps"), new Gauge[Int] {
      override def getValue: Int = {
        val cloudFSStats = FileSystem.getAllStatistics().asScala.find(s => s.getScheme.equals(fsName))
        cloudFSStats.map(_.getWriteOps).getOrElse(0)
      }
    })

  }

  // Return the plugin's driver-side component.
  // register metrics conditional to --conf spark.cernSparkPlugin.registerOnDriver=true
  override def driverPlugin(): DriverPlugin = {
    new DriverPlugin() {
      override def init(sc: SparkContext, myContext: PluginContext): JMap[String, String] = {
        val registerOnDriver =
          myContext.conf.getBoolean("spark.cernSparkPlugin.registerOnDriver", true)
        if (registerOnDriver) {
          cloudFilesystemMetrics(myContext)
        }
        Map.empty[String, String].asJava
      }
    }
  }

   // Return the plugin's executor-side component.
  override def executorPlugin(): ExecutorPlugin = {
    new ExecutorPlugin {
      override def init(myContext:PluginContext, extraConf:JMap[String, String])  = {
        cloudFilesystemMetrics(myContext)
      }
    }
  }

}

