packageArchetype.java_application

name := "gsn"

organization := "ch.epfl.lsir"

version := "1.1.3"

scalaVersion := "2.10.1"

crossPaths := false

libraryDependencies ++= Seq(
  "com.h2database" % "h2" % "1.1.116",
  "mysql" % "mysql-connector-java" % "5.1.29",
  "commons-dbcp" % "commons-dbcp" % "1.4",
  "org.apache.axis2" % "axis2-adb" % "1.5.5",
  "org.apache.httpcomponents" % "httpclient" % "4.3.2",
  "org.apache.commons" % "commons-email" % "1.3.2",
  "commons-io" % "commons-io" % "2.4",
  "org.apache.hbase" % "hbase" % "0.94.6.1",
  "org.apache.hadoop" % "hadoop-common" % "0.23.10",
  "log4j" % "log4j" % "1.2.17",
  "org.jibx" % "jibx-run" % "1.2.5",            
  "org.eclipse.jetty" % "jetty-webapp" % "7.0.2.v20100331",
  "org.hibernate" % "hibernate-core" % "3.6.10.Final",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "com.thoughtworks.xstream" % "xstream" % "1.4.5",
  "servlets.com" % "cos" % "05Nov2002",
  "javax.media" % "jmf" % "2.1.1e",
  "org.antlr" % "stringtemplate" % "3.0",
  "org.apache.mina" % "mina-core" % "1.1.7",
  "rome" % "rome" % "1.0",
  "junit" % "junit" % "4.11",
  "org.easymock" % "easymockclassextension" % "3.2",
  "org.httpunit" % "httpunit" % "1.7.2" intransitive
//  "org.jibx" % "jibx-bind" % "1.2.2",
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
    val dirs = (option / "tinyos") +++ (option / "charting") +++ (option / "compiling") +++ (option / "databases") +++ (option / "gis") +++ (option / "modeling") +++ (option / "numerical") +++ (option / "scriptlet") +++ (option / "semantic") +++ (option / "serial-port") +++ (option / "statistics") +++ (option / "twitter") +++ (option / "voip") +++ (libs / "jasper")  
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
