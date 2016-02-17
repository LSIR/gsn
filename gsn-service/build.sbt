import com.github.play2war.plugin._
import com.typesafe.sbt.packager.Keys._

name := "gsn-services"

organization := "gsn"

version := "1.0.0-SNAPSHOT"

Play2WarPlugin.play2WarSettings

Play2WarKeys.servletVersion := "2.5"

scalaVersion := "2.11.2"

crossPaths := false

scriptClasspath := Seq("*")

val buildSettings = Defaults.defaultSettings ++ Seq(
   javaOptions += "-Xmx128m",
   javaOptions += "-Xms64m"
)

libraryDependencies ++= Seq(
  jdbc,
  ws,
  cache,
  "com.h2database" % "h2" % "1.4.181",
  "mysql" % "mysql-connector-java" % "5.1.6",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",
  "org.scalatestplus" %% "play" % "1.1.0" % "test"
  )

publishTo := Some("Artifactory Realm" at "http://osper.epfl.ch:8081/artifactory/gsn-release")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true

publishArtifact in (Test) := false

publishArtifact in (Compile) := false

publishArtifact in (Compile, packageBin) := true
