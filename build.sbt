/****************************************
 *  spark‑plugins – build definition    *
 ****************************************/

// ─── Versions ────────────────────────────────────────────────────────────────
val sparkVersion   = "3.5.5"
val scala212       = "2.12.18"
val scala213       = "2.13.8"
val jacksonVersion = "2.18.3"
val slf4jVersion   = "2.0.17"

// ─── Project coordinates ────────────────────────────────────────────────────
name                 := "spark-plugins"
organization         := "ch.cern.sparkmeasure"
version              := "0.5-SNAPSHOT"
isSnapshot           := true
scalaVersion         := scala212
crossScalaVersions   := Seq(scala212, scala213)
publishMavenStyle    := true

// ─── Dependencies ────────────────────────────────────────────────────────────
// NOTE: Spark JARs are marked 'provided' because the cluster supplies them.
libraryDependencies ++= Seq(
  "org.apache.spark"            %% "spark-core"            % sparkVersion % Provided,
  "org.apache.spark"            %% "spark-sql"             % sparkVersion % Provided,
  "io.dropwizard.metrics"        % "metrics-core"          % "4.2.19",
  "org.apache.hadoop"            % "hadoop-client-api"     % "3.3.4",
  "io.pyroscope"                 % "agent"                 % "2.1.2",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "org.slf4j"                    % "slf4j-api"             % slf4jVersion
)

// ─── Publishing to Sonatype OSSRH ────────────────────────────────────────────
publishTo := Some {
  if (isSnapshot.value)
    Opts.resolver.sonatypeOssSnapshots.head
  else
    Opts.resolver.sonatypeStaging
}

// ─── Project metadata ─────────────────────────────────────────────────────────
organization := "ch.cern.sparkmeasure"
description := "Use Spark Plugins to extend Apache Spark with custom metrics and executors' startup actions."

homepage   := Some(url("https://github.com/cerndb/SparkPlugins"))
licenses   += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
developers := List(
  Developer(
    id    = "LucaCanali",
    name  = "Luca Canali",
    email = "Luca.Canali@cern.ch",
    url("https://github.com/LucaCanali")
  )
)
scmInfo := Some(
  ScmInfo(
    browseUrl  = url("https://github.com/cerndb/SparkPlugins"),
    connection = "scm:git@github.com:cerndb/SparkPlugins.git"
  )
)
