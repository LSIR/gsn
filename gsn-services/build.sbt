
name := "gsn-services"

//val buildSettings = Defaults.defaultSettings ++ Seq(
//   javaOptions += "-Xmx128m",
//   javaOptions += "-Xms64m"
//)

Compile / sources := Seq.empty
doc / sources := Seq.empty

libraryDependencies ++= Seq(
  "be.objectify"  %% "deadbolt-java"     % "2.7.1",
  "be.objectify"  %% "deadbolt-scala"     % "2.7.1",
  "com.esotericsoftware.kryo" % "kryo" % "2.23.0",
  "com.h2database" % "h2" % "1.4.181",
  "com.typesafe.play" %% "play" % "2.7.9",
  "com.typesafe.play" %% "play-logback" % "2.7.9",
  "com.typesafe.play" %% "play-cache" % "2.7.9",
  "com.typesafe.play" %% "play-java" % "2.7.9",
  "com.typesafe.akka" %% "akka-actor" % "2.5.32",
  "com.typesafe.play" %% "play-json" % "2.7.4",
  "com.typesafe.play" %% "play-jdbc" % "2.7.9",
  "com.typesafe.play" %% "play-ahc-ws" % "2.7.9",
  "mysql" % "mysql-connector-java" % "5.1.6",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2", //newest is 2.1.0
  "org.webjars" % "bootstrap" % "3.2.0",
  "org.zeromq" % "jeromq" % "0.3.5"

//  "org.scalatestplus" %% "play" % "1.1.0" % "test",
//  "com.nulab-inc" %% "play2-oauth2-provider" % "0.14.0",
//  "com.feth" % "play-authenticate_2.11" % "0.6.9",
//  "com.google.inject" % "guice" % "3.0",
//  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",
//  "org.scalatestplus" %% "play" % "1.1.0" % "test",
//  "ch.epfl.gsn" % "gsn-core" % "2.0.1" exclude("org.apache.logging.log4j", "log4j-slf4j-impl"),
//  "com.typesafe.play" %% "play-json" % "2.3.10",
//  "com.typesafe.akka" %% "akka-actor" % "2.3.14"
//  "be.objectify"  %% "deadbolt-java"     % "2.3.3",
//  "be.objectify"  %% "deadbolt-scala"     % "2.3.3",
//  "org.webjars" % "bootstrap" % "3.2.0",
//  "org.scalatestplus" %% "play" % "1.1.0" % "test",
//  "com.nulab-inc" %% "play2-oauth2-provider" % "0.14.0",
//  "com.feth" % "play-authenticate_2.11" % "0.6.9",
//  "com.google.inject" % "guice" % "3.0",
//  "com.esotericsoftware.kryo" % "kryo" % "2.23.0",
//  "org.zeromq" % "jeromq" % "0.3.5",
//  "org.reactivemongo" %% "reactivemongo" % "1.0.8"

//  "ch.epfl.gsn" % "gsn-core" % "2.0.1" exclude("org.apache.logging.log4j", "log4j-slf4j-impl"),
  )

//libraryDependencies := libraryDependencies.value.map(_.exclude("ch.qos.logback", "logback-classic").exclude("ch.qos.logback", "logback-core"))


com.typesafe.sbt.SbtNativePackager.Linux / NativePackagerKeys.packageSummary := "GSN Services"

NativePackagerKeys.packageDescription := "Global Sensor Networks Services"

com.typesafe.sbt.SbtNativePackager.Linux/ NativePackagerKeys.maintainer := "LSIR EPFL <gsn@epfl.ch>"

Debian / debianPackageDependencies += "java8-runtime"

Debian / debianPackageRecommends ++= Seq("postgresql", "gsn-core", "nginx")

//Debian / serverLoading:= ServerLoader.Systemd

Linux / daemonUser := "gsn"
