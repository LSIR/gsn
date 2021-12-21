//import scala.collection.JavaConverters._

name := "gsn-tools"

libraryDependencies ++= Seq(
  //  "org.scalatest" % "scalatest" % "2.2.1" % "test",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2", //newest is 2.1.0
  "org.scala-lang.modules" %% "scala-xml" % "1.3.0", //was 2.12 2.0.1
//  "org.scala-lang" % "scala-library" % "2.12.14",

  "org.slf4j" % "slf4j-api" % "1.7.32",
  "com.typesafe.play" %% "play" % "2.7.9",
  "com.typesafe.akka" %% "akka-actor" % "2.5.32",
  "com.typesafe.play" %% "play-json" % "2.7.4",
//  "com.typesafe.play" %% "play" % "2.8.10",
//  "com.typesafe.akka" %% "akka-actor" % "2.6.17",
//  "com.typesafe.play" %% "play-json" % "2.9.2",
//  "com.typesafe.play" %% "play-enhancer" % "1.2.2",

  "com.typesafe" % "config" % "1.4.1",
  "com.typesafe.slick" %% "slick" % "2.1.0", //"3.3.3",
  "com.typesafe.akka" %% "akka-actor" % "2.5.32",

  "com.mchange" % "c3p0" % "0.9.5.5",
  "org.apache.jena" % "jena-core" % "2.11.0" exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-log4j12"),
  // newer version seems to miss com.hp. ...
  //  "org.apache.jena" % "jena-core" % "4.3.1" exclude("log4j","log4j") exclude("org.slf4j","slf4j-log4j12"),

  "org.reactivemongo" %% "reactivemongo" % "1.0.8",
  //  "org.reactivemongo" %% "reactivemongo-bson" % "0.20.13",
  "org.reactivemongo" %% "reactivemongo-bson-api" % "1.0.8",

  "edu.ucar" % "netcdf" % "4.3.22",
  "org.geotools" % "gt-shapefile" % "26.0",
  "org.geotools" % "gt-geojson" % "26.0",
  "org.geotools" % "gt-epsg-hsql" % "26.0",

  // was moved
  "rome" % "rome" % "1.0",
  "com.rometools" % "rome" % "1.16.0",

  //  "org.scalatest" %% "scalatest" % "3.2.10" % "test",
  //  // "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test",
  //  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % "test",
  //
  //   "com.typesafe.akka" %% "akka-testkit" % "2.3.14" % "test", //was 2.12
  ////  "com.typesafe.akka" %% "akka-testkit" % "2.6.17" % "test",
  //
  //  "edu.ucar" % "netcdf" % "4.3.22",
  //
  // "javax.media" % "jai_core" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar",
  //  "javax.media" % "jai_core" % "1.1.3",
  //
  //  "org.apache.jena" % "jena-core" % "2.11.0" exclude("log4j","log4j") exclude("org.slf4j","slf4j-log4j12"),
  //
  //  "ch.qos.logback" % "logback-classic" % "1.1.1" % "test",
  //  "org.mindrot" % "jbcrypt" % "0.3m",
  //
  "com.vividsolutions" % "jts" % "1.13"
)

scalacOptions += "-deprecation"