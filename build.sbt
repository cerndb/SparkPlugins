name := "SparkPlugins"

version := "0.1"
scalaVersion := "2.12.10"

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

libraryDependencies += "io.dropwizard.metrics" % "metrics-core" % "4.1.1"

//libraryDependencies += "org.apache.hadoop" % "hadoop-common" % "2.7.4"
libraryDependencies += "org.apache.hadoop" % "hadoop-common" % "3.2.0"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.0.0"
