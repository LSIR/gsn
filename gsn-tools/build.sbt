name := "gsn-tools"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2",
  "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.2",  
  "com.typesafe.play" %% "play-json" % "2.3.10",
  "com.typesafe.akka" %% "akka-actor" % "2.3.14",
  "edu.ucar" % "netcdf" % "4.3.22",
  "org.geotools" % "gt-shapefile" % "13.2",
  "org.geotools" % "gt-geojson" % "13.2",
  "org.geotools" % "gt-epsg-hsql" % "13.2",
  "javax.media" % "jai_core" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar",
  "org.apache.jena" % "jena-core" % "2.11.0" exclude("log4j","log4j") exclude("org.slf4j","slf4j-log4j12"),
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.mchange" % "c3p0" % "0.9.5-pre10",
  "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23",
  "ch.qos.logback" % "logback-classic" % "1.1.1" % "test",
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.14" % "test",
  "org.mindrot" % "jbcrypt" % "0.3m"
)


scalacOptions += "-deprecation"
