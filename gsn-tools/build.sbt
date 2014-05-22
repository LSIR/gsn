name := "gsn-tools"

organization := "ch.epfl.lsir"

version := "0.0.1"

scalaVersion := "2.10.4"

crossPaths := false

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.13" ,
  "org.scalatest" % "scalatest_2.10" % "2.0.RC1" % "test"
)

resolvers ++= Seq(
  DefaultMavenRepository,
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

mainClass := Some("gsn.Main")

scalacOptions += "-deprecation"

//EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

parallelExecution in Test := false

//publishTo := Some("Artifactory Realm" at "http://aldebaran.dia.fi.upm.es/artifactory/sstreams-releases-local")

//credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true

publishArtifact in (Compile) := false
