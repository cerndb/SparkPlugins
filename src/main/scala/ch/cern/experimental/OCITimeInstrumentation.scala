package ch.cern.experimental

import java.util.{Map => JMap}

import com.codahale.metrics.{Gauge, MetricRegistry}

import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

// Custom instrumentation of OCI-HDFS connector I/0 with time measurements, needs a custom oci-dfs connector
class OCITimeInstrumentation extends SparkPlugin {

  // S3A metrics registration using Hadoop 2.7 API
  def ociMetrics(metricRegistry: MetricRegistry): Unit= {

    metricRegistry.register(MetricRegistry.name("OCIReadTimeMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        com.oracle.bmc.hdfs.store.BmcTimeInstrumentation.getTimeElapsedReadMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("OCISeekTimeMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        com.oracle.bmc.hdfs.store.BmcTimeInstrumentation.getTimeElapsedSeekMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("OCICPUTimeDuringReadMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        com.oracle.bmc.hdfs.store.BmcTimeInstrumentation.getCPUTimeDuringReadMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("OCICPUTimeDuringSeekMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        com.oracle.bmc.hdfs.store.BmcTimeInstrumentation.getCPUTimeDuringSeekMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("OCIReadTimeMinusCPUMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        com.oracle.bmc.hdfs.store.BmcTimeInstrumentation.getTimeElapsedReadMusec -
          com.oracle.bmc.hdfs.store.BmcTimeInstrumentation.getCPUTimeDuringReadMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("OCISeekTimeMinusCPUMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        com.oracle.bmc.hdfs.store.BmcTimeInstrumentation.getTimeElapsedSeekMusec -
          com.oracle.bmc.hdfs.store.BmcTimeInstrumentation.getCPUTimeDuringSeekMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("OCIBytesRead"), new Gauge[Long] {
      override def getValue: Long = {
        com.oracle.bmc.hdfs.store.BmcTimeInstrumentation.getBytesRead
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
        ociMetrics(myContext.metricRegistry)
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
          ociMetrics(myContext.metricRegistry)
        }
      }
    }
  }

}

