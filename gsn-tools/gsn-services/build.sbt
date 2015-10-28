import com.github.play2war.plugin._
//import com.typesafe.sbt.packager.Keys._

name := "gsn-services"

organization := "gsn"

version := "0.0.3-SNAPSHOT"

Play2WarPlugin.play2WarSettings

Play2WarKeys.servletVersion := "2.5"

scalaVersion := "2.11.2"

crossPaths := false

scriptClasspath := Seq("*")

resolvers += "play-authenticate (release)" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += "play-authenticate (snapshot)" at "https://oss.sonatype.org/content/repositories/snapshots/"


libraryDependencies ++= Seq(
  jdbc,
  ws,
  cache,
  "com.h2database" % "h2" % "1.4.181",
  "mysql" % "mysql-connector-java" % "5.1.6",
  "org.scalatestplus" %% "play" % "1.1.0" % "test",
  "com.nulab-inc" %% "play2-oauth2-provider" % "0.15.1",
  "com.feth" %% "play-authenticate" % "0.7.0-SNAPSHOT",
  "com.github.play2war.ext" %% "redirect-playlogger" % "1.0.1"
  )

publishTo := Some("Artifactory Realm" at "http://osper.epfl.ch:8081/artifactory/gsn-release")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true

publishArtifact in (Test) := false

publishArtifact in (Compile) := false

publishArtifact in (Compile, packageBin) := true
