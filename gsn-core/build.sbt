import NativePackagerHelper._

//import com.typesafe.sbt.packager.archetypes.ServerLoader

import com.typesafe.sbt.packager.archetypes.TemplateWriter

import com.typesafe.sbt.packager.linux._

name := "gsn-core"

Revolver.settings

libraryDependencies ++= Seq(
  "org.eclipse.californium" % "californium-core" % "1.0.4",
  "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.1.0",
  "com.esotericsoftware.kryo" % "kryo" % "2.23.0",
  "com.rometools" % "rome" % "1.16.0",
  "com.typesafe.play" %% "play" % "2.7.9",
  "com.typesafe.play" %% "play-json" % "2.7.4",
  "org.apache.httpcomponents" % "httpclient" % "4.3.2",
  "org.apache.commons" % "commons-email" % "1.3.2",
  "org.hibernate" % "hibernate-core" % "3.6.10.Final",
  "commons-collections" % "commons-collections" % "3.2.1",
  "commons-dbcp" % "commons-dbcp" % "1.4",
  "commons-io" % "commons-io" % "2.4",
  "commons-lang" % "commons-lang" % "2.6",
  "com.h2database" % "h2" % "1.4.181",
  "com.thoughtworks.xstream" % "xstream" % "1.4.5",
  "net.rforge" % "REngine" % "0.6-8.1",
  "net.rforge" % "Rserve" % "0.6-8.1",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "org.antlr" % "stringtemplate" % "3.0",
  "org.apache.logging.log4j" % "log4j-api" % "2.17.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.17.0",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.0",
  "org.apache.logging.log4j" % "log4j-web" % "2.17.0",
  "org.apache.logging.log4j" % "log4j-jul" % "2.17.0",
  "org.codehaus.groovy" % "groovy-all" % "2.2.2",
  "org.easymock" % "easymock" % "4.3",
  "org.jfree" % "jfreechart" % "1.0.19",
  "org.rxtx" % "rxtx" % "2.1.7",
  "org.slf4j" % "slf4j-api" % "1.7.32",
  "org.zeromq" % "jeromq" % "0.3.5"

//  "org.jfree" % "jcommon" % "1.0.23",
//  "junit" % "junit" % "4.11" %  "test",
//  "ch.epfl.gsn" % "gsn-tools" % "2.0.1",
//  "org.easymock" % "easymockclassextension" % "3.2" % "test",
//  "org.httpunit" % "httpunit" % "1.7.2" % "test" exclude("xerces","xercesImpl") exclude("xerces","xmlParserAPIs") exclude("javax.servlet","servlet-api")
  //  "com.typesafe" % "config" % "1.2.1",
  //  "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.2",
  //  "mysql" % "mysql-connector-java" % "5.1.29",
  //  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",

)

mainClass := Some("ch.epfl.gsn.Main")

com.typesafe.sbt.SbtNativePackager.Linux / NativePackagerKeys.packageSummary := "GSN Server"

com.typesafe.sbt.SbtNativePackager.Windows / NativePackagerKeys.packageSummary := "GSN Server"

NativePackagerKeys.packageDescription := "Global Sensor Networks Core"

com.typesafe.sbt.SbtNativePackager.Windows / NativePackagerKeys.maintainer := "LSIR EPFL <gsn@epfl.ch>"

com.typesafe.sbt.SbtNativePackager.Linux / NativePackagerKeys.maintainer := "LSIR EPFL <gsn@epfl.ch>"

Debian / debianPackageDependencies += "java11-runtime"

Debian / debianPackageRecommends ++= Seq("postgresql", "munin-node", "gsn-services")

//Debian / serverLoading := ServerLoader.Systemd
// serverLoading in Rpm := ServerLoader.Systemd
enablePlugins(SystemdPlugin)

Linux / daemonUser := "gsn"

//Universal / mappings += sourceDirectory map { src => (src / "templates" / "gsn-core") -> "bin/gsn-core" }
//Universal / mappings := { ("src" / "templates" / "gsn-core") -> "bin/gsn-core";  sourceDirectory }

//Universal / mappings += sourceDirectory map { src => (src / "main" / "resources" / "log4j2.xml") -> "conf/log4j2.xml" }
//Universal / mappings += { (src / "main" / "resources" / "log4j2.xml") -> "conf/log4j2.xml"; sourceDirectory}

//Universal / mappings += sourceDirectory map { src => (src / "main" / "resources" / "wrappers.properties") -> "conf/wrappers.properties" }
//Universal / mappings += { (src / "main" / "resources" / "wrappers.properties") -> "conf/wrappers.properties"; sourceDirectory}

//Universal / mappings += baseDirectory map { base => (base / ".." / "conf" / "gsn.xml") -> "conf/gsn.xml" }
//Universal / mappings += { (base / ".." / "conf" / "gsn.xml") -> "conf/gsn.xml"; baseDirectory}

//Debian / linuxPackageMappings += { (base / ".." / "virtual-sensors" / "packaged") -> "/usr/share/gsn-core/conf/virtual-sensors"; baseDirectory}

//Universal / mappings ++= baseDirectory map { base => 
//    ((base / ".." / "virtual-sensors" / "samples").***) pair {file => Some("virtual-sensors-samples/" + file.name)}
//}

//linuxPackageMappings := {
//    val mappings = linuxPackageMappings.value
//    mappings map { 
//        case linuxPackage if linuxPackage.fileData.config equals "true" =>
//            val newFileData = linuxPackage.fileData.copy(
//                user = "gsn"
//            )
//            linuxPackage.copy(
//                fileData = newFileData
//            )
//        case linuxPackage => linuxPackage
//    }
//}

scalacOptions += "-deprecation"

EclipseKeys.projectFlavor := EclipseProjectFlavor.Java

reStart / mainClass := Some("ch.epfl.gsn.Main")

reStartArgs := Seq("../conf", "../virtual-sensors")