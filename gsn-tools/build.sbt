name := "gsn-tools"

organization := "gsn"

version := "0.0.4-SNAPSHOT"

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
  "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.2",  
  "com.typesafe.play" %% "play-json" % "2.3.4",
  "edu.ucar" % "netcdf" % "4.3.22",
  "org.apache.jena" % "jena-core" % "2.11.0" exclude("log4j","log4j") exclude("org.slf4j","slf4j-log4j12"),
  "com.typesafe.slick" %% "slick" % "2.1.0",
    "ch.qos.logback" % "logback-classic" % "1.1.1" % "test",
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
)

resolvers ++= Seq(
  DefaultMavenRepository,
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "lsir remote" at "http://planetdata.epfl.ch:8081/artifactory/remote-repos"
)

scalacOptions += "-deprecation"

//EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

parallelExecution in Test := false

publishTo := Some("Artifactory Realm" at "http://planetdata.epfl.ch:8081/artifactory/gsn-release")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true

publishArtifact in (Test) := false

publishArtifact in (Compile) := false

publishArtifact in (Compile, packageBin) := true