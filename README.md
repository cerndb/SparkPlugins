# SparkPlugins
Spark Plugins code examples (for Apache Spark master and future Spark 3.0)
Author and contact: Luca.Canali@cern.ch 

- **Spark plugins** implement the org.apache.spark.api.Plugin interface and can be
used to run custom code at the startup of Spark executors and driver.  
This is useful for extending instrumentation and for advanced configuration setup.
Mode details at [Spark monitoring documentation](https://github.com/apache/spark/blob/master/docs/monitoring.md#advanced-instrumentation)  
Note: The code in this repo will not work with Spark 2.x,
follow the link for examples of [Executor Plugins for Spark 2.4](https://github.com/cerndb/SparkExecutorPlugins2.4)

**Example usage:**  
  build with `sbt package`
  ```
  /bin/spark-shell --master yarn --jars <path>/sparkplugins_2.12-0.1.jar \
  --conf spark.plugins=ch.cern.RunOSCommandPlugin 
  ```

  ```
  /bin/spark-shell --master yarn --jars <path>/sparkplugins_2.12-0.1.jar \
  --conf spark.plugins=spark.plugins=ch.cern.HDFSMetrics32 
  ```

**Notes**:  
This repo contains code and plugin example to extend Spark metrics instrumentation.   
For example for measuring S3 filesystem metrics and/or using HDFS extended statistics metrics 
(for Hadoop 2.8 and higher, following HADOOP-13065).
See [this blog article](https://db-blog.web.cern.ch/blog/luca-canali/2019-02-performance-dashboard-apache-spark)
for additional background on the Spark metrics system on how it can be used to build
Spark performance dashboards.

## Plugins: examples and filesystem instrumentation 
  - DemoPlugin
     - basic plugin, for demo and testing
  - DemoMetricsPlugin
    - example plugin illustrating integration with Spark metrics system 
  - RunOSCommandPlugin
     - simple plugin, illustrating startup action, for demo and testing
  - HDFSMetrics32
    - collects extended HDFS metrics using GlobalFilesystemStatistics API, use with Hadoop 2.8 and higher
  - OCIMetrics27
    - collects metrics for the OCI-hdfs-connector, an S3 wrapper for OCI, use with Hadoop 2.7 and higher 
  - ROOTMetrics27
    - collects metrics for the Hadoop-XRootD connector, used by CERN EOS storage, use with Hadoop 2.7 and higher 
  - S3AMetrics27 
     - collects S3A metrics, use with for Hadoop 2.7 and higher 
  - S3AMetrics32
     - collects extended S3A metrics, use with for Hadoop 2.8 and higher 

## Experimental plugins
- Experimental plugins exposing metrics for I/O-time instrumentation of Hadoop-compliant filesystems.
  These plugins use instrumented/experimental/custom versions of the Hadoop client API.  
  - EOSCustomMetrics -> collects metrics for the Hadoop-XRootD connector 
    - use with CERN EOS storage system
    - see: [Hadoop-XRootD connector instrumentation](https://github.com/cerndb/hadoop-xrootd/blob/master/src/main/java/ch/cern/eos/XRootDInstrumentation.java) 
  - OCICustomMetrics -> for Oracle OCI storage hdfs connector
    - see: [OCI-Hadoop connector instrumentation](https://github.com/LucaCanali/oci-hdfs-connector/blob/BMCInstrumentation/hdfs-connector/src/main/java/com/oracle/bmc/hdfs/store)
  - S3ATimeInstrumentation -> for Hadoop S3A client
    - see: [HDFS and S3A custom instrumentation](https://github.com/LucaCanali/hadoop/tree/s3aAndHDFSTimeInstrumentation)  
  - HDFSCustomMetrics -> for Hadoop HDFS client
    - see: [HDFS and S3A custom instrumentation](https://github.com/LucaCanali/hadoop/tree/s3aAndHDFSTimeInstrumentation)  
