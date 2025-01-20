import Dependencies._

ThisBuild / scalaVersion     := "2.13.12"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "trad-scala",
    libraryDependencies += munit % Test,
    libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.4.2",
    libraryDependencies += "com.opencsv" % "opencsv" % "5.10",
    libraryDependencies += "org.apache.spark" %% "spark-core" % "3.5.4",
    libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.5.4"
)

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
