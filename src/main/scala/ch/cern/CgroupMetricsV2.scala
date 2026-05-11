package ch.cern

import scala.jdk.CollectionConverters._
import java.nio.file.{Files, Path, Paths}
import java.util.{Map => JMap}

import com.codahale.metrics.{Gauge, MetricRegistry}
import org.apache.spark.SparkContext
import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}

import scala.io.Source

// Collects OS metrics from cgroups v2 unified hierarchy instrumentation.
// Use when running Spark on Linux distributions that mount cgroup2 under /sys/fs/cgroup.

class CgroupMetricsV2 extends SparkPlugin {

  private lazy val cgroupLayout = CgroupMetricsV2.detectCgroupLayout()

  def cgroupCPUMetrics(metricRegistry: MetricRegistry): Unit = {
    metricRegistry.register(MetricRegistry.name("CPUTimeNanosec"), new Gauge[Long] {
      override def getValue: Long = {
        CgroupMetricsV2.cpuTimeNanoseconds(cgroupLayout.map(_.cpuStatPath))
      }
    })
  }

  def cgroupMemoryMetrics(metricRegistry: MetricRegistry): Unit = {
    metricRegistry.register(MetricRegistry.name("MemoryRss"), new Gauge[Long] {
      override def getValue: Long = {
        CgroupMetricsV2.memoryStatValue(cgroupLayout.map(_.memoryStatPath), "anon")
      }
    })

    metricRegistry.register(MetricRegistry.name("MemorySwap"), new Gauge[Long] {
      override def getValue: Long = {
        CgroupMetricsV2.firstValue(cgroupLayout.map(_.memorySwapCurrentPath))
      }
    })

    metricRegistry.register(MetricRegistry.name("MemoryCache"), new Gauge[Long] {
      override def getValue: Long = {
        CgroupMetricsV2.memoryStatValue(cgroupLayout.map(_.memoryStatPath), "file")
      }
    })
  }

  def cgroupNetworkMetrics(metricRegistry: MetricRegistry): Unit = {
    val procFileName = "/proc/net/netstat"

    metricRegistry.register(MetricRegistry.name("NetworkBytesIn"), new Gauge[Long] {
      override def getValue: Long = {
        CgroupMetricsV2.networkStatValue(procFileName, "InOctets")
      }
    })

    metricRegistry.register(MetricRegistry.name("NetworkBytesOut"), new Gauge[Long] {
      override def getValue: Long = {
        CgroupMetricsV2.networkStatValue(procFileName, "OutOctets")
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

object CgroupMetricsV2 {
  case class CgroupEntry(path: String)
  case class CgroupLayout(
      cgroupPath: Path,
      cpuStatPath: Path,
      memoryStatPath: Path,
      memorySwapCurrentPath: Path)

  def detectCgroupLayout(
      procSelfCgroupLines: Seq[String] = readLines(Paths.get("/proc/self/cgroup")),
      mountInfoLines: Seq[String] = readLines(Paths.get("/proc/self/mountinfo")),
      exists: Path => Boolean = Files.exists(_)): Option[CgroupLayout] = {

    val cgroupEntry = parseProcSelfCgroup(procSelfCgroupLines).headOption
    val cgroupMount = parseMountInfo(mountInfoLines).find(_.fsType == "cgroup2")

    val detected = for {
      entry <- cgroupEntry
      mount <- cgroupMount
      cgroupPath = mount.mountPoint.resolve(cgroupRelativePath(mount.root, entry.path)).normalize()
      layout = layoutFromCgroupPath(cgroupPath)
      if exists(layout.cpuStatPath) && exists(layout.memoryStatPath)
    } yield layout

    detected.orElse(fallbackLayout(exists))
  }

  def cpuTimeNanoseconds(cpuStatPath: Option[Path]): Long = {
    memoryStatValue(cpuStatPath, "usage_usec") * 1000L
  }

  def memoryStatValue(memoryStatPath: Option[Path], key: String): Long = {
    memoryStatPath
      .map(readLines)
      .map(parseFlatKeyedLines)
      .flatMap(_.get(key))
      .getOrElse(0L)
  }

  def firstValue(path: Option[Path]): Long = {
    path.flatMap(firstLine).flatMap(parseLong).getOrElse(0L)
  }

  def networkStatValue(procFileName: String, key: String): Long = {
    CgroupMetrics.networkStatValue(procFileName, key)
  }

  def parseFlatKeyedLines(lines: Seq[String]): Map[String, Long] = {
    lines.flatMap { line =>
      splitFields(line) match {
        case Array(key, value) => parseLong(value).map(key -> _)
        case _ => None
      }
    }.toMap
  }

  def parseProcSelfCgroup(lines: Seq[String]): Seq[CgroupEntry] = {
    lines.flatMap { line =>
      line.split(":", 3) match {
        case Array("0", "", path) => Some(CgroupEntry(path))
        case _ => None
      }
    }
  }

  private def layoutFromCgroupPath(cgroupPath: Path): CgroupLayout = {
    CgroupLayout(
      cgroupPath,
      cgroupPath.resolve("cpu.stat"),
      cgroupPath.resolve("memory.stat"),
      cgroupPath.resolve("memory.swap.current")
    )
  }

  private def fallbackLayout(exists: Path => Boolean): Option[CgroupLayout] = {
    val layout = layoutFromCgroupPath(Paths.get("/sys/fs/cgroup"))
    if (exists(layout.cpuStatPath) && exists(layout.memoryStatPath)) Some(layout) else None
  }

  private case class MountInfo(root: String, mountPoint: Path, fsType: String)

  private def parseMountInfo(lines: Seq[String]): Seq[MountInfo] = {
    lines.flatMap { line =>
      val parts = line.split(" - ", 2)
      if (parts.length != 2) {
        None
      } else {
        val mountFields = splitFields(parts(0))
        val fsFields = splitFields(parts(1))
        if (mountFields.length >= 5 && fsFields.nonEmpty) {
          Some(MountInfo(
            unescapeMountInfo(mountFields(3)),
            Paths.get(unescapeMountInfo(mountFields(4))),
            fsFields(0)
          ))
        } else {
          None
        }
      }
    }
  }

  private def cgroupRelativePath(mountRoot: String, cgroupPath: String): String = {
    val root = stripSlashes(mountRoot)
    val path = stripSlashes(cgroupPath)
    if (root.nonEmpty && (path == root || path.startsWith(root + "/"))) {
      stripSlashes(path.stripPrefix(root))
    } else {
      path
    }
  }

  private def stripSlashes(value: String): String = {
    value.stripPrefix("/").stripSuffix("/")
  }

  private def readLines(path: Path): Seq[String] = {
    try {
      val source = Source.fromFile(path.toFile)
      try source.getLines().toList finally source.close()
    } catch {
      case _: Exception => Seq.empty
    }
  }

  private def firstLine(path: Path): Option[String] = {
    readLines(path).headOption
  }

  private def parseLong(value: String): Option[Long] = {
    try Some(value.toLong) catch {
      case _: NumberFormatException => None
    }
  }

  private def splitFields(line: String): Array[String] = {
    line.trim.split("\\s+").filter(_.nonEmpty)
  }

  private def unescapeMountInfo(value: String): String = {
    value
      .replace("\\040", " ")
      .replace("\\011", "\t")
      .replace("\\012", "\n")
      .replace("\\134", "\\")
  }
}
