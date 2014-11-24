name := "gsn-tools"

organization := "ch.epfl.lsir"

version := "0.0.2"

scalaVersion := "2.11.2"

crossPaths := false

lazy val gsnweb = (project in file("gsn-services")).enablePlugins(PlayScala).dependsOn(tools)

lazy val tools = (project in file("."))

lazy val root = project.
  aggregate(tools,gsnweb).
  settings(
    aggregate in update := false
  )

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2",
  "ch.qos.logback" % "logback-classic" % "1.0.13" ,
  "com.typesafe.play" %% "play-json" % "2.3.4",
  "edu.ucar" % "netcdf" % "4.3.22",
  "org.apache.jena" % "jena-core" % "2.11.0" exclude("log4j","log4j") exclude("org.slf4j","slf4j-log4j12"),  
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
)

resolvers ++= Seq(
  DefaultMavenRepository,
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "lsir remote" at "http://planetdata.epfl.ch:8081/artifactory/remote-repos"
)

mainClass := Some("gsn.Main")

scalacOptions += "-deprecation"

//EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

parallelExecution in Test := false

//publishTo := Some("Artifactory Realm" at "http://aldebaran.dia.fi.upm.es/artifactory/sstreams-releases-local")

//credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true

publishArtifact in (Compile) := false
