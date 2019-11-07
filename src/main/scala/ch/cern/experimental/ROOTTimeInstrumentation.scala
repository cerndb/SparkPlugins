package ch.cern.experimental

import java.util.{Map => JMap}

import com.codahale.metrics.{Gauge, MetricRegistry}

import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

// Custom instrumentation of Hadoop-XRootD connectore I/0 with time measurements
class ROOTTimeInstrumentation extends SparkPlugin {

  // S3A metrics registration using Hadoop 2.7 API
  def rootMetrics(metricRegistry: MetricRegistry): Unit= {

    metricRegistry.register(MetricRegistry.name("ROOTBytesRead"), new Gauge[Long] {
      override def getValue: Long = {
        ch.cern.eos.XRootDInstrumentation.getBytesRead
        }
      })


    metricRegistry.register(MetricRegistry.name("ROOTReadOps"), new Gauge[Int] {
      override def getValue: Int = {
        ch.cern.eos.XRootDInstrumentation.getReadOps
      }
    })

    metricRegistry.register(MetricRegistry.name("ROOTTimeElapsedReadMusec"), new Gauge[Long] {
      override def getValue: Long = {
        ch.cern.eos.XRootDInstrumentation.getTimeElapsedReadMusec
      }
    })

    /** temporarily disable write metrics
    metricRegistry.register(MetricRegistry.name("ROOTWriteOps"), new Gauge[Int] {
      override def getValue: Int = {
        ch.cern.eos.XRootDInstrumentation.getWriteOps
      }
    })

    metricRegistry.register(MetricRegistry.name("ROOTBytesWritten"), new Gauge[Long] {
      override def getValue: Long = {
        ch.cern.eos.XRootDInstrumentation.getBytesWritten
      }
    })

    metricRegistry.register(MetricRegistry.name("ROOTTimeElapsedWriteMusec"), new Gauge[Long] {
      override def getValue: Long = {
        ch.cern.eos.XRootDInstrumentation.getTimeElapsedWriteMusec
      }
    })
    */
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
        rootMetrics(myContext.metricRegistry)
      }
    }
  }

}

