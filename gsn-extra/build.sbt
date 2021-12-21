import com.typesafe.sbt.SbtNativePackager._

name := "gsn-extra"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2", //newest is 2.1.0
  "com.typesafe.play" %% "play" % "2.7.9",
  "com.typesafe.akka" %% "akka-actor" % "2.5.32",
  "com.typesafe.play" %% "play-json" % "2.7.4",
  "com.typesafe" % "config" % "1.4.1",
  "com.typesafe.slick" %% "slick" % "2.1.0", //"3.3.3",
  "com.typesafe.akka" %% "akka-actor" % "2.5.32",
  "com.mchange" % "c3p0" % "0.9.5.5",
  "org.apache.jena" % "jena-core" % "2.11.0" exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-log4j12"),
  "org.reactivemongo" %% "reactivemongo" % "1.0.8",
  "org.reactivemongo" %% "reactivemongo-bson-api" % "1.0.8",
  "edu.ucar" % "netcdf" % "4.3.22",
  "org.geotools" % "gt-shapefile" % "26.0",
  "org.geotools" % "gt-geojson" % "26.0",
  "org.geotools" % "gt-epsg-hsql" % "26.0",
  "com.h2database" % "h2" % "1.4.181",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "com.thoughtworks.xstream" % "xstream" % "1.4.5",
  "com.rometools" % "rome" % "1.16.0",
  "org.jfree" % "jfreechart" % "1.0.19",
  "org.codehaus.groovy" % "groovy-all" % "2.2.2",
  "net.rforge" % "REngine" % "0.6-8.1",
  "net.rforge" % "Rserve" % "0.6-8.1",
  "com.esotericsoftware.kryo" % "kryo" % "2.23.0",
  "org.zeromq" % "jeromq" % "0.3.5",
  "com.typesafe.play" %% "play" % "2.7.9",
  "com.typesafe.play" %% "play-json" % "2.7.4",

  "org.scala-lang.modules" %% "scala-xml" % "1.3.0", //was 2.12 2.0.1
  "com.h2database" % "h2" % "1.4.181",
  "mysql" % "mysql-connector-java" % "5.1.29",
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  "commons-dbcp" % "commons-dbcp" % "1.4",
  "commons-collections" % "commons-collections" % "3.2.1",
  "commons-io" % "commons-io" % "2.4",
  "commons-lang" % "commons-lang" % "2.6",
  "rome" % "rome" % "1.0",
  "com.rometools" % "rome" % "1.16.0",
  "org.hibernate" % "hibernate-core" % "3.6.10.Final",
  "org.apache.httpcomponents" % "httpclient" % "4.3.2",
  "org.apache.commons" % "commons-email" % "1.3.2",
  "org.apache.axis2" % "axis2-adb" % "1.5.5" exclude("javax.servlet","servlet-api"),
  "org.apache.hbase" % "hbase-common" % "0.99.0",
  "org.apache.hbase" % "hbase-client" % "0.99.0",
  "org.apache.hadoop" % "hadoop-core" % "1.2.1" exclude("com.sun.jersey","jersey-core") exclude ("com.sun.jersey","jersey-server") exclude ("com.sun.jersey","jersey-json"),
  "org.slf4j" % "slf4j-api" % "1.7.32",
  "org.antlr" % "stringtemplate" % "3.0",
  "org.rxtx" % "rxtx" % "2.1.7",
  "org.zeromq" % "jeromq" % "0.3.5",

  "org.eclipse.jetty" % "jetty-webapp" % "7.6.16.v20140903",
  "javax.media" % "jmf" % "2.1.1e",
  "nz.ac.waikato.cms.weka" % "weka-stable" % "3.6.6",
  "org.asteriskjava" % "asterisk-java" % "1.0.0.M3",
  "jasperreports" % "jasperreports" % "3.5.3",

  //  "log4j" % "log4j" % "1.2.17",
  //  "org.slf4j" % "slf4j-log4j12" % "1.7.5",
  //  "org.jibx" % "jibx-run" % "1.2.5",
  //  "com.thoughtworks.xstream" % "xstream" % "1.4.5",
  //  "servlets.com" % "cos" % "05Nov2002",
  //  "org.apache.mina" % "mina-core" % "1.1.7",
  //  "org.glassfish.jersey.containers" % "jersey-container-servlet-core" % "2.8",
  //  "org.glassfish.jersey.core" % "jersey-client" % "2.8",
  //  "com.ganyo" % "gcm-server" % "1.0.2",
  //  "org.jfree" % "jcommon" % "1.0.23",
  //  "com.vividsolutions" % "jts" % "1.13",
  //  "org.postgis" % "postgis-jdbc" % "1.3.3",
  //  //"nz.ac.waikato.cms.weka" % "LibSVM" % "1.0.6",
  //  "org.codehaus.groovy" % "groovy-all" % "2.2.2",
  //  "net.rforge" % "REngine" % "0.6-8.1",
  //  "net.rforge" % "Rserve" % "0.6-8.1",
    "org.nuiton.thirdparty" % "JRI" % "0.8-4"
  //  "com.esotericsoftware.kryo" % "kryo" % "2.23.0",
  //  "junit" % "junit" % "4.11" %  "test",
  //  "ch.epfl.gsn" % "gsn-core" % "2.0.1",
  //  "org.easymock" % "easymockclassextension" % "3.2" % "test",
  //  "org.httpunit" % "httpunit" % "1.7.2" % "test" exclude("xerces","xercesImpl") exclude("xerces","xmlParserAPIs") exclude("javax.servlet","servlet-api")
)


//Compile / unmanagedJars += baseDirectory.value / "lib" / "optional" / "tinyos"
//Compile / unmanagedJars += baseDirectory.value / "optional"
//Compile / unmanagedJars += baseDirectory.value / "tinyos"

unmanagedBase := baseDirectory.value / "lib" / "optional"


//val dirs = (option / "legacy") +++ (option / "tinyos") +++ (option / "numerical")
//    (dirs ** "*.jar").classpath

scalacOptions += "-deprecation"

EclipseKeys.projectFlavor := EclipseProjectFlavor.Java
