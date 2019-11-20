package ch.cern.experimental

import java.util.{Map => JMap}

import com.codahale.metrics.{Gauge, MetricRegistry}

import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

// Custom instrumentation of HDFS I/0 with time measurements, needs custom hdfs client
class HDFSTimeInstrumentation extends SparkPlugin {

  // S3A metrics registration using Hadoop 2.7 API
  def hdfsMetrics(metricRegistry: MetricRegistry): Unit= {

    metricRegistry.register(MetricRegistry.name("HDFSReadTimeMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        org.apache.hadoop.hdfs.HDFSTimeInstrumentation.getTimeElapsedReadMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("HDFSCPUTimeDuringReadMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        org.apache.hadoop.hdfs.HDFSTimeInstrumentation.getCPUTimeDuringReadMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("HDFSReadTimeMinusCPUMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        org.apache.hadoop.hdfs.HDFSTimeInstrumentation.getTimeElapsedReadMusec -
          org.apache.hadoop.hdfs.HDFSTimeInstrumentation.getCPUTimeDuringReadMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("HDFSBytesRead"), new Gauge[Long] {
      override def getValue: Long = {
        org.apache.hadoop.hdfs.HDFSTimeInstrumentation.getBytesRead
      }
    })

    metricRegistry.register(MetricRegistry.name("HDFSReadCalls"), new Gauge[Int] {
      override def getValue: Int = {
        org.apache.hadoop.hdfs.HDFSTimeInstrumentation.getReadCalls
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
        // Don't register executor plugin if in local mode
        if (! myContext.conf.get("spark.master").startsWith("local")) {
          hdfsMetrics(myContext.metricRegistry)
        }
      }
    }
  }

}

