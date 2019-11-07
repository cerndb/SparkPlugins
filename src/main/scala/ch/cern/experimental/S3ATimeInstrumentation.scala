package ch.cern.experimental

import java.util.{Map => JMap}

import com.codahale.metrics.{Gauge, MetricRegistry}

import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}
import org.apache.spark.SparkContext

// Custom instrumentation of S3A I/0 with time measurements, needs custom s3a client
class S3ATimeInstrumentation extends SparkPlugin {

  // S3A metrics registration using Hadoop 2.7 API
  def s3aMetrics(metricRegistry: MetricRegistry): Unit= {

    metricRegistry.register(MetricRegistry.name("S3AReadTimeMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        org.apache.hadoop.fs.s3a.S3ATimeInstrumentation.getTimeElapsedReadMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("S3ASeekTimeMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        org.apache.hadoop.fs.s3a.S3ATimeInstrumentation.getTimeElapsedSeekMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("S3ACPUTimeDuringReadMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        org.apache.hadoop.fs.s3a.S3ATimeInstrumentation.getCPUTimeDuringReadMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("S3ACPUTimeDuringSeekMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        org.apache.hadoop.fs.s3a.S3ATimeInstrumentation.getCPUTimeDuringSeekMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("S3AReadTimeMinusCPUMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        org.apache.hadoop.fs.s3a.S3ATimeInstrumentation.getTimeElapsedReadMusec -
          org.apache.hadoop.fs.s3a.S3ATimeInstrumentation.getCPUTimeDuringReadMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("S3ASeekTimeMinusCPUMuSec"), new Gauge[Long] {
      override def getValue: Long = {
        org.apache.hadoop.fs.s3a.S3ATimeInstrumentation.getTimeElapsedSeekMusec -
          org.apache.hadoop.fs.s3a.S3ATimeInstrumentation.getCPUTimeDuringSeekMusec
      }
    })

    metricRegistry.register(MetricRegistry.name("S3ABytesRead"), new Gauge[Long] {
      override def getValue: Long = {
        org.apache.hadoop.fs.s3a.S3ATimeInstrumentation.getBytesRead
      }
    })

    metricRegistry.register(MetricRegistry.name("S3AReadCalls"), new Gauge[Int] {
      override def getValue: Int = {
        org.apache.hadoop.fs.s3a.S3ATimeInstrumentation.getReadCalls
      }
    })

    metricRegistry.register(MetricRegistry.name("S3ASeekCalls"), new Gauge[Int] {
      override def getValue: Int = {
        org.apache.hadoop.fs.s3a.S3ATimeInstrumentation.getSeekCalls
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

