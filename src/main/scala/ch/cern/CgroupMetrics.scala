package ch.cern

import java.util.{Map => JMap}
import scala.collection.JavaConverters._

import com.codahale.metrics.{Gauge, MetricRegistry}
import org.apache.spark.SparkContext
import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}

import scala.io.Source

// Collects OS metrics from group instrumentation
// Use when running Spark on docker containers, notably with Kubernetes to get the container CPU usage
// When used outside containers or other cgroup caging, it will report the full system CPU usage

class CgroupMetrics extends SparkPlugin {

  def cgroupCPUMetrics(metricRegistry: MetricRegistry): Unit = {
    val procFileName = "/sys/fs/cgroup/cpuacct/cpuacct.usage"
    metricRegistry.register(MetricRegistry.name("CPUTimeNanosec"), new Gauge[Long] {
      override def getValue: Long = {
        val procFile = Source.fromFile(procFileName)
        val firstLine = procFile.getLines().next
        val cpuTimeNs = firstLine.toLong
        procFile.close()
        cpuTimeNs
      }
    })

  }

  def cgroupMemoryMetrics(metricRegistry: MetricRegistry): Unit = {
    val procFileName = "/sys/fs/cgroup/memory/memory.stat"

    metricRegistry.register(MetricRegistry.name("MemoryRss"), new Gauge[Long] {
      override def getValue: Long = {
        val procFile = Source.fromFile(procFileName)
        val data = procFile.getLines.filter(_.contains("total_rss"))
          .map(_.split(" ")).map { case Array(k, v) => k -> v.toLong }.toMap
        val rssMemory = data("total_rss")
        procFile.close()
        rssMemory
      }
    })

    metricRegistry.register(MetricRegistry.name("MemorySwap"), new Gauge[Long] {
      override def getValue: Long = {
        val procFile = Source.fromFile(procFileName)
        val data = procFile.getLines.filter(_.contains("total_swap"))
          .map(_.split(" ")).map { case Array(k, v) => k -> v.toLong }.toMap
        val swapMemory = data("total_swap")
        procFile.close()
        swapMemory
      }
    })

    metricRegistry.register(MetricRegistry.name("MemoryCache"), new Gauge[Long] {
      override def getValue: Long = {
        val procFile = Source.fromFile(procFileName)
        val data = procFile.getLines.filter(_.contains("total_cache"))
          .map(_.split(" ")).map { case Array(k, v) => k -> v.toLong }.toMap
        val cacheMemory = data("total_cache")
        procFile.close()
        cacheMemory
      }
    })

  }

  def cgroupNetworkMetrics(metricRegistry: MetricRegistry): Unit = {
    val procFileName = "/proc/net/netstat"

    metricRegistry.register(MetricRegistry.name("NetworkBytesIn"), new Gauge[Long] {
      override def getValue: Long = {
        val procFile = Source.fromFile(procFileName)
        val data = procFile.getLines.toList.map(_.split(" "))
        val headersIpExt = data(2)
        val dataIpExt = data(3)
        val inOctets = dataIpExt(headersIpExt.indexOf("InOctets")).toLong
        procFile.close()
        inOctets
      }
    })

    metricRegistry.register(MetricRegistry.name("NetworkBytesOut"), new Gauge[Long] {
      override def getValue: Long = {
        val procFile = Source.fromFile(procFileName)
        val data = procFile.getLines.toList.map(_.split(" "))
        val headersIpExt = data(2)
        val dataIpExt = data(3)
        val outOctets = dataIpExt(headersIpExt.indexOf("OutOctets")).toLong
        procFile.close()
        outOctets
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
          cgroupCPUMetrics(myContext.metricRegistry)
          cgroupMemoryMetrics(myContext.metricRegistry)
          cgroupNetworkMetrics(myContext.metricRegistry)
        }
        Map.empty[String, String].asJava
      }
    }
  }

  // Return the plugin's executor-side component.
  override def executorPlugin(): ExecutorPlugin = {
    new ExecutorPlugin() {
      override def init(myContext: PluginContext, extraConf: JMap[String, String]): Unit = {
        cgroupCPUMetrics(myContext.metricRegistry)
        cgroupMemoryMetrics(myContext.metricRegistry)
        cgroupNetworkMetrics(myContext.metricRegistry)
      }
    }
  }

}
