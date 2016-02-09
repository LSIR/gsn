import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

name := "gsn"

organization := "gsn"

version := "2.0.0"

packageArchetype.java_application

packAutoSettings

Revolver.settings

scalaVersion := "2.11.2"

crossPaths := false

libraryDependencies ++= Seq(
  "gsn" % "gsn-tools" % "0.0.4-SNAPSHOT" exclude("org.slf4j" ,"slf4j-nop") ,
  "gsn" % "gsn-services" % "0.0.3-SNAPSHOT" exclude("org.slf4j" ,"slf4j-nop") ,
  "com.typesafe" % "config" % "1.2.1",
  "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.2",
  "com.h2database" % "h2" % "1.4.181",
  "mysql" % "mysql-connector-java" % "5.1.29",
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  "commons-dbcp" % "commons-dbcp" % "1.4",
  "org.hibernate" % "hibernate-core" % "3.6.10.Final",
  //"org.apache.axis2" % "axis2-adb" % "1.5.5" exclude("javax.servlet","servlet-api"),
  "org.apache.httpcomponents" % "httpclient" % "4.3.2",
  "org.apache.commons" % "commons-email" % "1.3.2",
  "commons-io" % "commons-io" % "2.4",
  //"org.apache.hbase" % "hbase-common" % "0.99.0",
  //"org.apache.hbase" % "hbase-client" % "0.99.0",
  //"org.apache.hadoop" % "hadoop-core" % "1.2.1" exclude("com.sun.jersey","jersey-core") exclude ("com.sun.jersey","jersey-server") exclude ("com.sun.jersey","jersey-json"),
  "log4j" % "log4j" % "1.2.17",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-log4j12" % "1.7.5",
  //"org.jibx" % "jibx-run" % "1.2.5",            
  "org.eclipse.jetty" % "jetty-webapp" % "7.6.16.v20140903",
  "org.hibernate" % "hibernate-core" % "3.6.10.Final",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "com.thoughtworks.xstream" % "xstream" % "1.4.5",
  //"servlets.com" % "cos" % "05Nov2002", 
  //"javax.media" % "jmf" % "2.1.1e",
  "org.antlr" % "stringtemplate" % "3.0",
  //"org.apache.mina" % "mina-core" % "1.1.7",
  "commons-lang" % "commons-lang" % "2.6",
  "rome" % "rome" % "1.0",
  //"org.glassfish.jersey.containers" % "jersey-container-servlet-core" % "2.8",
  //"org.glassfish.jersey.core" % "jersey-client" % "2.8",
  //"com.ganyo" % "gcm-server" % "1.0.2",
  "org.jfree" % "jfreechart" % "1.0.19", 
  "org.jfree" % "jcommon" % "1.0.23",
  //"com.vividsolutions" % "jts" % "1.13",
  //"org.postgis" % "postgis-jdbc" % "1.3.3",
  //"nz.ac.waikato.cms.weka" % "weka-stable" % "3.6.6",
  //"nz.ac.waikato.cms.weka" % "LibSVM" % "1.0.6",
  //"org.asteriskjava" % "asterisk-java" % "1.0.0.M3",
  //"jasperreports" % "jasperreports" % "3.5.3",
  "org.codehaus.groovy" % "groovy-all" % "2.2.2",
  "net.rforge" % "REngine" % "0.6-8.1",
  "net.rforge" % "Rserve" % "0.6-8.1",
  //"org.nuiton.thirdparty" % "JRI" % "0.8-4",
  "org.rxtx" % "rxtx" % "2.1.7",
  "com.esotericsoftware.kryo" % "kryo" % "2.23.0",
  "org.zeromq" % "jeromq" % "0.3.0",
  "junit" % "junit" % "4.11" %  "test",
  "org.easymock" % "easymockclassextension" % "3.2" % "test",
  "org.httpunit" % "httpunit" % "1.7.2" % "test" exclude("xerces","xercesImpl") exclude("xerces","xmlParserAPIs") exclude("javax.servlet","servlet-api")
)

resolvers ++= Seq(
  DefaultMavenRepository,
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "LSIR" at "http://osper.epfl.ch:8081/artifactory/gsn-release",
  "Local ivy Repository" at ""+Path.userHome.asFile.toURI.toURL+"/.ivy2/local" 
)

mainClass := Some("gsn.Main")

NativePackagerKeys.packageSummary in Linux := "GSN Server"

NativePackagerKeys.packageSummary in Windows := "GSN Server"

NativePackagerKeys.packageDescription := "Global Sensor Networks"

NativePackagerKeys.maintainer in Windows := "LSIR EPFL"

NativePackagerKeys.maintainer in Debian := "LSIR EPFL"

scalacOptions += "-deprecation"

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

EclipseKeys.projectFlavor := EclipseProjectFlavor.Java

parallelExecution in Test := false

publishTo := Some("Artifactory Realm" at "http://osper.epfl.ch:8081/artifactory/gsn-release")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true

publishArtifact in (Compile) := false

mainClass in Revolver.reStart := Some("gsn.Main")

Revolver.reStartArgs := Seq("22232")
