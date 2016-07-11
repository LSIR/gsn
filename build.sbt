
lazy val commonSettings = Seq(
  organization := "gsn",
  version := "2.0.0-SNAPSHOT",
  scalaVersion := "2.11.2",
  javacOptions ++= Seq("-source", "1.7", "-target", "1.7", "-bootclasspath", "/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/rt.jar"),
  resolvers ++= Seq(
    DefaultMavenRepository,
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    "LSIR" at "http://osper.epfl.ch:8081/artifactory/gsn-release",
    "LSIR remote" at "http://osper.epfl.ch:8081/artifactory/remote-repos",
    "osgeo" at "http://download.osgeo.org/webdav/geotools/",
    "play-authenticate (release)" at "https://oss.sonatype.org/content/repositories/releases/",
    "play-authenticate (snapshot)" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "Local ivy Repository" at ""+Path.userHome.asFile.toURI.toURL+"/.ivy2/local"
  ),
  publishTo := Some("Artifactory Realm" at "http://osper.epfl.ch:8081/artifactory/gsn-release"),
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  publishMavenStyle := true,
  publishArtifact in (Compile) := false,
  publishArtifact in (Test) := false,
  publishArtifact in (Compile, packageBin) := true,
  crossPaths := false,
  parallelExecution in Test := false,
  EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource
)

lazy val root = (project in file(".")).
  aggregate(core, tools, extra, services)


lazy val core = (project in file("gsn-core")).
  dependsOn(tools).
  settings(commonSettings: _*).
  enablePlugins(JavaServerAppPackaging, DebianPlugin)

lazy val extra = (project in file("gsn-extra")).
  dependsOn(core).
  settings(commonSettings: _*)

lazy val services = (project in file("gsn-services")).
  dependsOn(tools).
  settings(commonSettings: _*).
  enablePlugins(PlayScala, DebianPlugin)

lazy val tools = (project in file("gsn-tools")).
  settings(commonSettings: _*)

lazy val webui = (project in file("gsn-webui")).
  enablePlugins(JavaServerAppPackaging, DebianPlugin)


lazy val startAll = taskKey[Unit]("Start all the GSN modules")


//startAll := {
  //(webui/startDjango in webui).value
//  (re-start in core).value
//  (run in services).value
//}
