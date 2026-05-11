package ch.cern

import scala.jdk.CollectionConverters._
import java.nio.file.{Files, Path, Paths}
import java.util.{Map => JMap}

import com.codahale.metrics.{Gauge, MetricRegistry}
import org.apache.spark.SparkContext
import org.apache.spark.api.plugin.{DriverPlugin, ExecutorPlugin, PluginContext, SparkPlugin}

import scala.io.Source

// Collects OS metrics from group instrumentation
// Use when running Spark on docker containers, notably with Kubernetes to get the container CPU usage
// When used outside containers or other cgroup caging, it will report the full system CPU usage

class CgroupMetrics extends SparkPlugin {

  private lazy val cgroupLayout = CgroupMetrics.detectCgroupV1Layout()

  def cgroupCPUMetrics(metricRegistry: MetricRegistry): Unit = {
    metricRegistry.register(MetricRegistry.name("CPUTimeNanosec"), new Gauge[Long] {
      override def getValue: Long = {
        CgroupMetrics.cpuTimeNanoseconds(cgroupLayout.map(_.cpuUsagePath))
      }
    })

  }

  def cgroupMemoryMetrics(metricRegistry: MetricRegistry): Unit = {
    metricRegistry.register(MetricRegistry.name("MemoryRss"), new Gauge[Long] {
      override def getValue: Long = {
        CgroupMetrics.memoryStatValue(cgroupLayout.map(_.memoryStatPath), "total_rss")
      }
    })

    metricRegistry.register(MetricRegistry.name("MemorySwap"), new Gauge[Long] {
      override def getValue: Long = {
        CgroupMetrics.memoryStatValue(cgroupLayout.map(_.memoryStatPath), "total_swap")
      }
    })

    metricRegistry.register(MetricRegistry.name("MemoryCache"), new Gauge[Long] {
      override def getValue: Long = {
        CgroupMetrics.memoryStatValue(cgroupLayout.map(_.memoryStatPath), "total_cache")
      }
    })

  }

  def cgroupNetworkMetrics(metricRegistry: MetricRegistry): Unit = {
    val procFileName = "/proc/net/netstat"

    metricRegistry.register(MetricRegistry.name("NetworkBytesIn"), new Gauge[Long] {
      override def getValue: Long = {
        CgroupMetrics.networkStatValue(procFileName, "InOctets")
      }
    })

    metricRegistry.register(MetricRegistry.name("NetworkBytesOut"), new Gauge[Long] {
      override def getValue: Long = {
        CgroupMetrics.networkStatValue(procFileName, "OutOctets")
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

object CgroupMetrics {
  case class CgroupEntry(controllers: Set[String], path: String)
  case class MountInfo(root: String, mountPoint: Path, fsType: String, superOptions: Set[String])
  case class CgroupV1Layout(cpuUsagePath: Path, memoryStatPath: Path)

  def detectCgroupV1Layout(
      procSelfCgroupLines: Seq[String] = readLines(Paths.get("/proc/self/cgroup")),
      mountInfoLines: Seq[String] = readLines(Paths.get("/proc/self/mountinfo")),
      exists: Path => Boolean = Files.exists(_)): Option[CgroupV1Layout] = {

    val entries = parseProcSelfCgroup(procSelfCgroupLines)
    val mounts = parseMountInfo(mountInfoLines).filter(_.fsType == "cgroup")

    val cpuEntry = entries.find(_.controllers.contains("cpuacct"))
      .orElse(entries.find(_.controllers.contains("cpu")))
    val memoryEntry = entries.find(_.controllers.contains("memory"))

    val cpuMount = mounts.find(_.superOptions.contains("cpuacct"))
      .orElse(mounts.find(_.superOptions.contains("cpu")))
    val memoryMount = mounts.find(_.superOptions.contains("memory"))

    val detected = for {
      cpuCgroup <- cpuEntry
      memoryCgroup <- memoryEntry
      cpuMountInfo <- cpuMount
      memoryMountInfo <- memoryMount
      cpuPath = cpuMountInfo.mountPoint
        .resolve(cgroupRelativePath(cpuMountInfo.root, cpuCgroup.path))
        .resolve("cpuacct.usage")
        .normalize()
      memoryPath = memoryMountInfo.mountPoint
        .resolve(cgroupRelativePath(memoryMountInfo.root, memoryCgroup.path))
        .resolve("memory.stat")
        .normalize()
      if exists(cpuPath) && exists(memoryPath)
    } yield CgroupV1Layout(cpuPath, memoryPath)

    detected.orElse(fallbackV1Layout(exists))
  }

  def cpuTimeNanoseconds(cpuUsagePath: Option[Path]): Long = {
    cpuUsagePath.flatMap(path => firstLine(path).flatMap(parseLong)).getOrElse(0L)
  }

  def memoryStatValue(memoryStatPath: Option[Path], key: String): Long = {
    memoryStatPath
      .map(readLines)
      .map(parseFlatKeyedLines)
      .flatMap(_.get(key))
      .getOrElse(0L)
  }

  def networkStatValue(procFileName: String, key: String): Long = {
    val ipExtLines = readLines(Paths.get(procFileName)).filter(_.startsWith("IpExt:"))
    ipExtLines.sliding(2).collectFirst {
      case Seq(headers, values) =>
        val headerFields = splitFields(headers).drop(1)
        val valueFields = splitFields(values).drop(1)
        val index = headerFields.indexOf(key)
        if (index >= 0 && index < valueFields.length) parseLong(valueFields(index)).getOrElse(0L) else 0L
    }.getOrElse(0L)
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
        case Array(_, controllers, path) =>
          Some(CgroupEntry(controllers.split(",").filter(_.nonEmpty).toSet, path))
        case _ => None
      }
    }
  }

  def parseMountInfo(lines: Seq[String]): Seq[MountInfo] = {
    lines.flatMap { line =>
      val parts = line.split(" - ", 2)
      if (parts.length != 2) {
        None
      } else {
        val mountFields = splitFields(parts(0))
        val fsFields = splitFields(parts(1))
        if (mountFields.length >= 5 && fsFields.length >= 3) {
          Some(MountInfo(
            unescapeMountInfo(mountFields(3)),
            Paths.get(unescapeMountInfo(mountFields(4))),
            fsFields(0),
            fsFields.drop(2).flatMap(_.split(",")).filter(_.nonEmpty).toSet
          ))
        } else {
          None
        }
      }
    }
  }

  private def fallbackV1Layout(exists: Path => Boolean): Option[CgroupV1Layout] = {
    val candidates = Seq(
      CgroupV1Layout(
        Paths.get("/sys/fs/cgroup/cpuacct/cpuacct.usage"),
        Paths.get("/sys/fs/cgroup/memory/memory.stat")
      ),
      CgroupV1Layout(
        Paths.get("/sys/fs/cgroup/cpu,cpuacct/cpuacct.usage"),
        Paths.get("/sys/fs/cgroup/memory/memory.stat")
      )
    )
    candidates.find(layout => exists(layout.cpuUsagePath) && exists(layout.memoryStatPath))
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
