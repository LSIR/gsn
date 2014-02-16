packageArchetype.java_application

name := "gsn"

organization := "ch.epfl.lsir"

version := "1.1.3"

scalaVersion := "2.10.1"

crossPaths := false

libraryDependencies ++= Seq(
  "log4j" % "log4j" % "1.2.17"
//  "org.jibx" % "jibx-bind" % "1.2.2",
//  "commons-collections" % "commons-collections" % "3.2.1",
)

resolvers ++= Seq(
  DefaultMavenRepository,
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

mainClass := Some("gsn.Main")

unmanagedSourceDirectories in Compile <<= (javaSource in Compile)(Seq(_))

unmanagedSourceDirectories in Test <<= (javaSource in Test)(Seq(_))

javaSource in Compile <<= baseDirectory(base => base / "src")

javaSource in Test :=  baseDirectory.value / "test"

resourceDirectory in Compile := baseDirectory.value / "conf"

resourceDirectory in Test := baseDirectory.value / "logs"

unmanagedJars in Compile <++= baseDirectory map { base =>
    val libs = base / "lib"
    val option = libs / "optional"
    val dirs = (libs / "core") +++ (libs / "core" / "jetty") +++ (option / "tinyos") +++ (option / "charting") +++ (option / "compiling") +++ (option / "databases") +++ (option / "gis") +++ (option / "modeling") +++ (option / "numerical") +++ (option / "scriptlet") +++ (option / "semantic") +++ (option / "serial-port") +++ (option / "statistics") +++ (option / "twitter") +++ (option / "voip") +++ (libs / "axis-2") +++ (libs / "hbase") +++ (libs / "hibernate") +++ (libs / "jasper") +++ (libs / "safe-storage") +++ (libs / "rss") +++ (libs / "junit-jars") +++ (libs / "slf") 
    (dirs ** "*.jar").classpath
}

NativePackagerKeys.packageSummary in Linux := "GSN Server"

NativePackagerKeys.packageSummary in Windows := "GSN Server"

NativePackagerKeys.packageDescription := "Global Sensor Networks"

NativePackagerKeys.maintainer in Windows := "LSIR EPFL"

NativePackagerKeys.maintainer in Debian := "LSIR EPFL"

scalacOptions += "-deprecation"

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

parallelExecution in Test := false

publishTo := Some("Artifactory Realm" at "http://aldebaran.dia.fi.upm.es/artifactory/sstreams-releases-local")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true

publishArtifact in (Compile) := false
