# SparkPlugins
![SparkPlugins CI](https://github.com/cerndb/SparkPlugins/workflows/SparkPlugins%20CI/badge.svg)
[![DOI](https://zenodo.org/badge/219974405.svg)](https://doi.org/10.5281/zenodo.15374785)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ch.cern.sparkmeasure/spark-plugins_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ch.cern.sparkmeasure/spark-plugins_2.12)

**Spark Plugins** are an Apache Spark feature for extending Spark with custom metrics and actions.
This repository provides ready-to-use examples for deploying Spark plugins across various use cases.
### Key Features

* **Custom Metrics:** Extend Spark's instrumentation with user-defined metrics.
* **Executor Actions:** Trigger custom actions upon executor startup, useful for integrations (e.g., monitoring systems).
* **Resource Monitoring:** Measure Spark’s usage of cluster resources (YARN, K8S, Standalone).
* **I/O Metrics:** Monitor I/O performance from cloud filesystems, OS metrics, and custom application metrics.
* **External Integrations:** Connect with external systems like Pyroscope for performance insights.

### Contents
- [Getting started](#getting-started---your-first-spark-plugins)
- [Demo and basic plugins](#demo-and-basic-plugins)
- [Implementation notes](#implementation-notes)
- [Plugin for integrating Pyroscope with Spark](#plugin-for-integrating-with-pyroscope)
- [Plugin for OS metrics instrumentation with Cgroups for Spark on Kubernetes](#os-metrics-instrumentation-with-cgroups-for-spark-on-kubernetes)
- [Plugin to collect I/O storage statistics for HDFS and Hadoop-compatible filesystems](#plugins-to-collect-io-storage-statistics-for-hdfs-and-hadoop-compatible-filesystems)
- [Plugin for Cloud filesystem storage statistics](#cloud-filesystem-storage-statistics-for-hadoop-compatible-filesystems)
- [Experimental plugins](#experimental-plugins-for-io-time-instrumentation)

### Resources

- Spark Performance Dashboard - a solution to ingest and visualize Spark metrics
    - link to the repo on [how to deploy a Spark Performance Dashboard using Spark metrics](https://github.com/cerndb/spark-dashboard)
- DATA+AI summit 2020 talk [What is New with Apache Spark Performance Monitoring in Spark 3.0](https://databricks.com/session_eu20/what-is-new-with-apache-spark-performance-monitoring-in-spark-3-0)
- DATA+AI summit 2021 talk [Monitor Apache Spark 3 on Kubernetes using Metrics and Plugins](https://databricks.com/session_na21/monitor-apache-spark-3-on-kubernetes-using-metrics-and-plugins)

Author and contact: Luca.Canali@cern.ch 

---
## Getting Started - Your First Spark Plugins

To begin using Spark plugins from this repository, follow these steps:

### 1. Deploying Spark Plugins

You can deploy the Spark plugins directly using Maven Central:

#### For Scala 2.12:

```bash
spark-shell --packages ch.cern.sparkmeasure:spark-plugins_2.12:0.4
```

#### For Scala 2.13:

```bash
spark-shell --packages ch.cern.sparkmeasure:spark-plugins_2.13:0.4
```

### 2. Building or Downloading the SparkPlugin JAR

You can either build the JAR from the source or download it directly:

#### Option 1: Build from Source

* Make sure you have [SBT (Scala Build Tool)](https://www.scala-sbt.org/) installed.
* Run the following command:

```bash
sbt +package
```

#### Option 2: Download from GitHub 

* See jars in the [Release version 0.4](https://github.com/cerndb/SparkPlugins/releases/tag/v0.4)
* Or visit the [GitHub Actions page](https://github.com/cerndb/SparkPlugins/actions) for this repository and 
  locate the latest successful build and download the JAR file.

---

### Demo and Basic Plugins
  - [DemoPlugin](src/main/scala/ch/cern/DemoPlugin.scala)
    - `--packages ch.cern.sparkmeasure:spark-plugins_2.12:0.4 --conf spark.plugins=ch.cern.DemoPlugin`
    - Basic plugin, demonstrates how to write Spark plugins in Scala, for demo and testing.
  - [DemoMetricsPlugin](src/main/scala/ch/cern/DemoMetricsPlugin.scala)
    - `--packages ch.cern.sparkmeasure:spark-plugins_2.12:0.4 --conf spark.plugins=ch.cern.DemoMetricsPlugin`
    - Example plugin illustrating integration with the Spark metrics system.
    - Metrics implemented:
      - `ch.cern.DemoMetricsPlugin.DriverTest42`: a gauge reporting a constant integer value, for testing.
  - [RunOSCommandPlugin](src/main/scala/ch/cern/RunOSCommandPlugin.scala)
    - `--conf spark.plugins=ch.cern.RunOSCommandPlugin`
    - Example illustrating how to use plugins to run actions on the OS.
    - Action implemented: runs an OS command on the executors, by default it runs: `/usr/bin/touch /tmp/plugin.txt`
    - Configurable action: `--conf spark.cernSparkPlugin.command="command or script you want to run"`
    - Example:
      ```
      bin/spark-shell --master yarn \ 
        --packages ch.cern.sparkmeasure:spark-plugins_2.12:0.4 \
        --conf spark.plugins=ch.cern.RunOSCommandPlugin 
      ```
        - You can see if the plugin has run by checking that the file `/tmp/plugin.txt` has been
          created on the executor machines. 
---
### Implementation Notes:
- Spark plugins implement the `org.apache.spark.api.Plugin` interface, they can be written in Scala or Java
  and can be used to run custom code at the startup of Spark executors and driver.
- Plugins basic configuration: `--conf spark.plugins=<list of plugin classes>`
- Plugin JARs need to be made available to Spark executors
    - you can distribute the plugin code to the executors using `--jars` and `--packages`
    - for K8S you can also consider making the jars available directly in the container image
- Most of the Plugins described in this repo are intended to extend the Spark Metrics System
    - See the details on the Spark metrics system at  [Spark Monitoring documentation](https://spark.apache.org/docs/latest/monitoring.html#metrics).
    - You can find the metrics generated by the plugins in the Spark metrics system stream under the
      namespace `namespace=plugin.<Plugin Class Name>`
- See also: [SPARK-29397](https://issues.apache.org/jira/browse/SPARK-29397), [SPARK-28091](https://issues.apache.org/jira/browse/SPARK-28091), [SPARK-32119](https://issues.apache.org/jira/browse/SPARK-32119).

---
## Plugins in this Repository

### Plugin for integrating with Pyroscope
[Grafana Pyroscope](https://grafana.com/oss/pyroscope/) is a tool for continuous profiling and Flame Graph visualization. This plugin allows to integrate Apache Spark and Pyroscope.
For details see:  
[How to profile Spark with Pyroscope](https://github.com/LucaCanali/Miscellaneous/blob/master/Spark_Notes/Tools_Spark_Pyroscope_FlameGraph.md) 

An example of how to put all the configuration together and start Spark on a cluster with Pyroscope Flame Graph
continuous monitoring. Example:
1. Start Pyroscope
- Download from https://github.com/grafana/pyroscope/releases
- CLI start: `./pyroscope -server.http-listen-port 5040`
- Or use docker: `docker run -it -p 5040:4040 grafana/pyroscope`  

2. Spark Spark (spark-shell, PySpark, spark-submit
```
bin/spark-shell --master yarn  \
  --packages ch.cern.sparkmeasure:spark-plugins_2.12:0.4,io.pyroscope:agent:2.1.2 \ # update to use the latest versions
  --conf spark.plugins=ch.cern.PyroscopePlugin \
  --conf spark.pyroscope.server="http://<myhostname>:5040" # match with the server and port used when starting Pyroscope
```

**Spark configurations:**  
This plugin adds the following configurations:
```
  --conf spark.pyroscope.server - > default "http://localhost:4040", update to match the server name and port used by Pyroscope
  --conf spark.pyroscope.applicationName -> default spark.conf.get("spark.app.id")
  --conf spark.pyroscope.eventType -> default ITIMER, possible values ITIMER, CPU, WALL, ALLOC, LOCK
```

**Example:**  
This is an example of how to use the configuration programmatically (using PySpark):
```
from pyspark.sql import SparkSession

# Get the Spark session
spark = (SparkSession.builder.
      appName("Instrumented app").master("yarn")
      .config("spark.executor.memory","16g")
      .config("spark.executor.cores","4")
      .config("spark.executor.instances", 2)
      .config("spark.jars.packages", "ch.cern.sparkmeasure:spark-plugins_2.12:0.4,io.pyroscope:agent:0.13.0")
      .config("spark.plugins", "ch.cern.PyroscopePlugin")
      .config("spark.pyroscope.server", "http://<myhostname>:5040")
      .getOrCreate()
    )
```
---
### OS metrics instrumentation with cgroups, for Spark on Kubernetes 
  - [CgroupMetrics](src/main/scala/ch/cern/CgroupMetrics.scala)
    - Configure with: `--conf spark.plugins=ch.cern.CgroupMetrics`
    - Optional configuration: `--conf spark.cernSparkPlugin.registerOnDriver` (default false)
    - Implemented using cgroup instrumentation of key system resource usage, intended mostly for
      Spark on Kubernetes
    - Collects metrics using CGroup stats from `/sys/fs` and from `/proc` filesystem 
      for CPU, Memory and Network usage. See also [kernel documentation](https://www.kernel.org/doc/Documentation/cgroup-v1)
      Note: the metrics are reported for the entire cgroup to which the executor belongs to. This is mostly
      intended for Spark running on Kubernetes. In other cases, the metrics reported
      may not be easily correlated with executor's activity, as the cgroup metrics may include more
      processes, up to the entire system.
    - Metrics implemented (gauges), with prefix `ch.cern.CgroupMetrics`:
      - `CPUTimeNanosec`: reports the CPU time used by the processes in the cgroup.
      - `MemoryRss`: number of bytes of anonymous and swap cache memory.
      - `MemorySwap`: number of bytes of swap usage.
      - `MemoryCache`: number of bytes of page cache memory.
      - `NetworkBytesIn`: network traffic inbound.
      - `NetworkBytesOut`: network traffic outbound.

    - Example:
    ```
    bin/spark-shell --master k8s://https://<K8S URL>:6443 --driver-memory 1g \ 
      --num-executors 2 --executor-cores 2 --executor-memory 2g \
      --conf spark.kubernetes.container.image=<registry>/spark:v355 \
      --packages ch.cern.sparkmeasure:spark-plugins_2.12:0.4 \
      --conf spark.plugins=ch.cern.HDFSMetrics,ch.cern.CgroupMetrics \
      --conf "spark.metrics.conf.*.sink.graphite.class"="org.apache.spark.metrics.sink.GraphiteSink"   \
      --conf "spark.metrics.conf.*.sink.graphite.host"=mytestinstance \
      --conf "spark.metrics.conf.*.sink.graphite.port"=2003 \
      --conf "spark.metrics.conf.*.sink.graphite.period"=10 \
      --conf "spark.metrics.conf.*.sink.graphite.unit"=seconds \
      --conf "spark.metrics.conf.*.sink.graphite.prefix"="youridhere"
    ```

   - Visualize the metrics using the [Spark dashboard](https://github.com/cerndb/spark-dashboard),
     see `Spark_Perf_Dashboard_v03_with_SparkPlugins`

---
### Plugins to collect I/O storage statistics for HDFS and Hadoop Compatible Filesystems

#### HDFS extended storage statistics
This Plugin measures HDFS extended statistics.
In particular, it provides information on read locality and erasure coding usage (for HDFS 3.x).

  - [HDFSMetrics](src/main/scala/ch/cern/HDFSMetrics.scala)
    - Configure with: `--conf spark.plugins=ch.cern.HDFSMetrics`
    - Optional configuration: `--conf spark.cernSparkPlugin.registerOnDriver` (default true)
    - Collects extended HDFS metrics using Hadoop's [GlobalStorageStatistics](https://hadoop.apache.org/docs/current/api/org/apache/hadoop/fs/GlobalStorageStatistics.html)
      implemented using Hadoop extended statistics metrics introduced in Hadoop 2.8.
    - Use this with Spark built with Hadoop 3.2 or higher
      (it does not work with Spark built with Hadoop 2.7).
    - Metrics (gauges) implemented have the prefix `ch.cern.HDFSMetrics`. List of metrics:
       - `bytesRead`
       - `bytesWritten`
       - `readOps`
       - `writeOps`
       - `largeReadOps`
       - `bytesReadLocalHost`
       - `bytesReadDistanceOfOneOrTwo`
       - `bytesReadDistanceOfThreeOrFour`
       - `bytesReadDistanceOfFiveOrLarger`
       - `bytesReadErasureCoded`

    - Example  
    ```
    bin/spark-shell --master yarn \
      --packages ch.cern.sparkmeasure:spark-plugins_2.12:0.4 \
      --conf spark.plugins=ch.cern.HDFSMetrics \
      --conf "spark.metrics.conf.*.sink.graphite.class"="org.apache.spark.metrics.sink.GraphiteSink"   \
      --conf "spark.metrics.conf.*.sink.graphite.host"=mytestinstance \
      --conf "spark.metrics.conf.*.sink.graphite.port"=2003 \
      --conf "spark.metrics.conf.*.sink.graphite.period"=10 \
      --conf "spark.metrics.conf.*.sink.graphite.unit"=seconds \
      --conf "spark.metrics.conf.*.sink.graphite.prefix"="youridhere"
    ```

   - Visualize the metrics using the [Spark dashboard](https://github.com/cerndb/spark-dashboard),
     see `Spark_Perf_Dashboard_v03_with_SparkPlugins`

---
### Cloud filesystem storage statistics for Hadoop Compatible Filesystems
#### [CloudFSMetrics](src/main/scala/ch/cern/CloudFSMetrics.scala)
This Plugin provides I/O statistics for Cloud Filesystem metrics (for s3a, gs, wasbs, oci, root, and any other
storage system exposed as a Hadoop Compatible Filesystem). 

  - Configure with: 
    - `--conf spark.plugins=ch.cern.CloudFSMetrics`
    - `--conf spark.cernSparkPlugin.cloudFsName=<name of the filesystem>` (example: "s3a", "gs", "wasbs", "root", "oci", etc.) 
    - Optional configuration: `--conf spark.cernSparkPlugin.registerOnDriver` (default true)  
    - Collects I/O metrics for Hadoop-compatible filesystems using Hadoop's GlobalStorageStatistics API.   
      - Note: use this with Spark built with Hadoop 3.x (requires Hadoop client version 2.8 or higher).
      - Spark also allows to measure filesystem metrics using
        `--conf spark.executor.metrics.fileSystemSchemes=<filesystems to measure>` (default: `file,hdfs`)
        however in Spark (up to 3.1) this is done using Hadoop Filesystem getAllStatistics, deprecated in recent versions of Hadoop.
    - Metrics (gauges) implemented have the prefix `ch.cern.S3AMetricsGSS`. List of metrics:
       - `bytesRead`
       - `bytesWritten`
       - `readOps`
       - `writeOps`
    - Example:
         ```
         bin/spark-shell --master k8s://https://<K8S URL>:6443 --driver-memory 1g \ 
          --num-executors 2 --executor-cores 2 --executor-memory 2g \
          --conf spark.kubernetes.container.image=<registry>/spark:v311 \
          --packages org.apache.hadoop:hadoop-aws:3.3.2,ch.cern.sparkmeasure:spark-plugins_2.12:0.4 \
          --conf spark.plugins=ch.cern.CloudFSMetrics,ch.cern.CgroupMetrics \
          --conf spark.cernSparkPlugin.cloudFsName="s3a" \
          --conf spark.hadoop.fs.s3a.secret.key="<SECRET KEY HERE>" \
          --conf spark.hadoop.fs.s3a.access.key="<ACCESS KEY HERE>" \
          --conf spark.hadoop.fs.s3a.endpoint="https://<S3A URL HERE>" \
          --conf spark.hadoop.fs.s3a.impl="org.apache.hadoop.fs.s3a.S3AFileSystem" \
          --conf "spark.metrics.conf.*.sink.graphite.class"="org.apache.spark.metrics.sink.GraphiteSink"   \
          --conf "spark.metrics.conf.*.sink.graphite.host"=mytestinstance \
          --conf "spark.metrics.conf.*.sink.graphite.port"=2003 \
          --conf "spark.metrics.conf.*.sink.graphite.period"=10 \
          --conf "spark.metrics.conf.*.sink.graphite.unit"=seconds \
          --conf "spark.metrics.conf.*.sink.graphite.prefix"="youridhere"
         ```

    - Visualize the metrics using the [Spark dashboard](https://github.com/cerndb/spark-dashboard),
      see `Spark_Perf_Dashboard_v03_with_SparkPlugins`

---
## Experimental Plugins for I/O Time Instrumentation

This section details a few experimental Spark plugins used to expose metrics for I/O-time
instrumentation of Spark workloads using Hadoop-compliant filesystems.  
These plugins use instrumented experimental/custom versions of the Hadoop client API for HDFS and other Hadoop-Compliant File Systems.

  - [S3A Time Instrumentation](src/main/scala/ch/cern/experimental/S3ATimeInstrumentation.scala) 
    - Instruments the Hadoop S3A client.
    - Note: this requires custom S3A client implementation, see experimental code at: [HDFS and S3A custom instrumentation](https://github.com/LucaCanali/hadoop/tree/s3aAndHDFSTimeInstrumentation)  
    - Spark config:
      - **Use this with Spark 3.1.x (which uses hadoop version 3.2.0)** 
      - `--conf spark.plugins=ch.cern.experimental.S3ATimeInstrumentation`
      - Custom jar needed: `--jars hadoop-aws-3.2.0.jar` 
        - build [from this fork](https://github.com/LucaCanali/hadoop/tree/s3aAndHDFSTimeInstrumentation)
        - or download a pre-built copy of the [hadoop-aws-3.2.0.jar at this link](https://cern.ch/canali/res/hadoop-aws-3.2.0.jar)

    - Metrics implemented (gauges), with prefix `ch.cern.experimental.S3ATimeInstrumentation`:
        - `S3AReadTimeMuSec`
        - `S3ASeekTimeMuSec`
        - `S3ACPUTimeDuringReadMuSec`
        - `S3ACPUTimeDuringSeekMuSec`
        - `S3AReadTimeMinusCPUMuSec`
        - `S3ASeekTimeMinusCPUMuSec`
        - `S3ABytesRead`
        - `S3AGetObjectMetadataMuSec`
        - `S3AGetObjectMetadataMinusCPUMuSec`

    - Example:
      ```
      bin/spark-shell --master k8s://https://<K8S URL>:6443 --driver-memory 1g \ 
       --num-executors 2 --executor-cores 2 --executor-memory 2g \
       --conf spark.kubernetes.container.image=<registry>/spark:v311 \
       --jars <PATH>/hadoop-aws-3.2.0.jar
       --packages com.amazonaws:aws-java-sdk-bundle:1.11.880,ch.cern.sparkmeasure:spark-plugins_2.12:0.1 \
       --conf spark.hadoop.fs.s3a.secret.key="<SECRET KEY HERE>" \
       --conf spark.hadoop.fs.s3a.access.key="<ACCESS KEY HERE>" \
       --conf spark.hadoop.fs.s3a.endpoint="https://<URL HERE>" \
       --conf spark.hadoop.fs.s3a.impl="org.apache.hadoop.fs.s3a.S3AFileSystem" \
       --conf "spark.metrics.conf.*.sink.graphite.class"="org.apache.spark.metrics.sink.GraphiteSink"   \
       --conf "spark.metrics.conf.*.sink.graphite.host"=mytestinstance \
       --conf "spark.metrics.conf.*.sink.graphite.port"=2003 \
       --conf "spark.metrics.conf.*.sink.graphite.period"=10 \
       --conf "spark.metrics.conf.*.sink.graphite.unit"=seconds \
       --conf "spark.metrics.conf.*.sink.graphite.prefix"="youridhere"
      ```

   - Visualize the metrics using the [Spark dashboard](https://github.com/cerndb/spark-dashboard),
     see `Spark_Perf_Dashboard_v03_with_SparkPlugins_Experimental`

  - [HDFS Time Instrumentation](src/main/scala/ch/cern/experimental/HDFSTimeInstrumentation.scala) 
    - Instruments the Hadoop HDFS client.
    - Note: this requires custom HDFS client implementation, see experimental code at: [HDFS and S3A custom instrumentation](https://github.com/LucaCanali/hadoop/tree/s3aAndHDFSTimeInstrumentation)
    - Spark config:
        - **Use this with Spark 3.1.x (which uses hadoop version 3.2.0)** 
        - `--conf spark.plugins=ch.cern.experimental.HDFSTimeInstrumentation`
      - `--packages ch.cern.sparkmeasure:spark-plugins_2.12:0.1`
      - Non-standard configuration required for using this instrumentation:  
        - replace `$SPARK_HOME/jars/hadoop-hdfs-client-3.2.0.jar` with the jar built [from this fork](https://github.com/LucaCanali/hadoop/tree/s3aAndHDFSTimeInstrumentation)
        - for convenience, you can download a pre-built copy of the [hadoop-hdfs-client-3.2.0.jar at this link](https://cern.ch/canali/res/hadoop-hdfs-client-3.2.0.jar)

    - Metrics implemented (gauges), with prefix `ch.cern.experimental.HDFSTimeInstrumentation`:
        - `HDFSReadTimeMuSec`
        - `HDFSCPUTimeDuringReadMuSec`
        - `HDFSReadTimeMinusCPUMuSec`
        - `HDFSBytesRead`
        - `HDFSReadCalls`
    - Example:
    ```
    bin/spark-shell --master yarn --num-executors 2 --executor-cores 2 \
     --jars <PATH>/sparkplugins_2.12-0.1.jar \
     --conf spark.plugins=ch.cern.experimental.HDFSTimeInstrumentation 
     ...NOTE: ADD here spark.metrics.conf parameters or configure metrics.conf> 
    ```        
    - Visualize the metrics with the Spark dashboard `spark_perf_dashboard_spark3-0_v02_with_sparkplugins_experimental`

  - [Hadoop-XRootD Time Instrumentation](src/main/scala/ch/cern/experimental/ROOTTimeInstrumentation.scala) 
    - Collects metrics for the Hadoop-XRootD connector 
    - Intended use is when using Spark with the CERN EOS (and CERN Box) storage system.
    - Additional details: [Hadoop-XRootD connector instrumentation](https://github.com/cerndb/hadoop-xrootd/blob/master/src/main/java/ch/cern/eos/XRootDInstrumentation.java) 
    - Spark config:
      ```
      --conf spark.plugins=ch.cern.experimental.ROOTTimeInstrumentation \
      --packages ch.cern.sparkmeasure:spark-plugins_2.12:0.1 \
      --conf spark.driver.extraClassPath=<path>/hadoop-xrootd-1.0.5.jar \
      --conf spark.executor.extraClassPath=<path>/hadoop-xrootd-1.0.5.jar \
      --files <path_to_krbtgt>#krbcache \
      --conf spark.executorEnv.KRB5CCNAME="FILE:krbcache"
      ```
    - Metrics implemented (gauges), with prefix `ch.cern.experimental.ROOTTimeInstrumentation`:
        - `ROOTBytesRead`
        - `ROOTReadOps`
        - `ROOTReadTimeMuSec`

  - Visualize the metrics using the [Spark dashboard](https://github.com/cerndb/spark-dashboard),
    see `Spark_Perf_Dashboard_v03_with_SparkPlugins_Experimental`
        
   - [OCITimeInstrumentation](src/main/scala/ch/cern/experimental/OCITimeInstrumentation.scala) 
    - instruments the HDFS connector to Oracle OCI storage.
    - Note: this requires a custom hdfs-oci connector implementation, see experimental code at:
     [OCI-Hadoop connector instrumentation](https://github.com/LucaCanali/oci-hdfs-connector/blob/BMCInstrumentation/hdfs-connector/src/main/java/com/oracle/bmc/hdfs/store)
    - Spark config:
      - `--conf spark.plugins=ch.cern.experimental.OCITimeInstrumentation`
      - Hack required for testing using Spark on Kubernetes: 
        copy `oci-hdfs-connector-2.9.2.6.jar` built [from this fork](https://github.com/LucaCanali/oci-hdfs-connector/)
        into `$SPARK_HOME/jars`
        copy and install the relevant [oracle/oci-java-sdk release jars](oci-java-sdk release jars):
          version 1.17.5: oci-java-sdk-full-1.17.5.jar and related third party jars.
      ```
      --packages ch.cern.sparkmeasure:spark-plugins_2.12:0.1 \ # Note for K8S rather add this to the container \
      --conf spark.hadoop.fs.oci.client.auth.pemfilepath="<PATH>/oci_api_key.pem" \
      --conf spark.hadoop.fs.oci.client.auth.tenantId=ocid1.tenancy.oc1..TENNANT_KEY \
      --conf spark.hadoop.fs.oci.client.auth.userId=ocid1.user.oc1.USER_KEY \
      --conf spark.hadoop.fs.oci.client.auth.fingerprint=<fingerprint_here> \
      --conf spark.hadoop.fs.oci.client.hostname="https://objectstorage.REGION_NAME.oraclecloud.com" \
      ```
     - Metrics implemented (gauges), with prefix `ch.cern.experimental.OCITimeInstrumentation`:
         - `OCIReadTimeMuSec`
         - `OCISeekTimeMuSec`
         - `OCICPUTimeDuringReadMuSec`
         - `OCICPUTimeDuringSeekMuSec`
         - `OCIReadTimeMinusCPUMuSec`
         - `OCISeekTimeMinusCPUMuSec`
         - `OCIBytesRead`
