name := "gsn-services"

organization := "gsn"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.2"

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

crossPaths := false

val buildSettings = Defaults.defaultSettings ++ Seq(
   javaOptions += "-Xmx128m",
   javaOptions += "-Xms64m"
)

libraryDependencies ++= Seq(
  jdbc,
  ws,
  cache,
  javaEbean,
  "com.h2database" % "h2" % "1.4.181",
  "mysql" % "mysql-connector-java" % "5.1.6",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "be.objectify"  %% "deadbolt-java"     % "2.3.3",
  "be.objectify"  %% "deadbolt-scala"     % "2.3.3",
  "org.webjars" % "bootstrap" % "3.2.0",
  "org.scalatestplus" %% "play" % "1.1.0" % "test",
  "com.nulab-inc" %% "play2-oauth2-provider" % "0.14.0",
  "com.feth" % "play-authenticate_2.11" % "0.6.9",
  "com.google.inject" % "guice" % "3.0",
  javaCore,
  "com.esotericsoftware.kryo" % "kryo" % "2.23.0",
  "com.typesafe.akka" % "akka-zeromq_2.11" % "2.3.14",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",
  "org.scalatestplus" %% "play" % "1.1.0" % "test",
  "gsn" % "gsn-tools" % "1.0.0-SNAPSHOT",
  "com.typesafe.play" %% "play-json" % "2.3.10",
  "com.typesafe.akka" %% "akka-actor" % "2.3.14"
  )

resolvers ++= Seq(
  DefaultMavenRepository,
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "lsir remote" at "http://osper.epfl.ch:8081/artifactory/remote-repos",
  "lsir release" at "http://osper.epfl.ch:8081/artifactory/gsn-release",
  "osgeo" at "http://download.osgeo.org/webdav/geotools/",
  "play-authenticate (release)" at "https://oss.sonatype.org/content/repositories/releases/",
  "play-authenticate (snapshot)" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

publishTo := Some("Artifactory Realm" at "http://osper.epfl.ch:8081/artifactory/gsn-release")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true

publishArtifact in (Test) := false

publishArtifact in (Compile) := false

publishArtifact in (Compile, packageBin) := true
