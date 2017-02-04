import com.typesafe.sbt.packager.archetypes.ServerLoader

name := "gsn-services"


val buildSettings = Defaults.defaultSettings ++ Seq(
   javaOptions += "-Xmx128m",
   javaOptions += "-Xms64m"
)

sources in (Compile,doc) := Seq.empty

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
  "org.zeromq" % "jeromq" % "0.3.5",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",
  "org.scalatestplus" %% "play" % "1.1.0" % "test",
  "ch.epfl.gsn" % "gsn-core" % "2.0.1" exclude("org.apache.logging.log4j", "log4j-slf4j-impl"),
  "com.typesafe.play" %% "play-json" % "2.3.10",
  "com.typesafe.akka" %% "akka-actor" % "2.3.14"
  )

//libraryDependencies := libraryDependencies.value.map(_.exclude("ch.qos.logback", "logback-classic").exclude("ch.qos.logback", "logback-core"))


NativePackagerKeys.packageSummary in com.typesafe.sbt.SbtNativePackager.Linux := "GSN Services"

NativePackagerKeys.packageDescription := "Global Sensor Networks Services"

NativePackagerKeys.maintainer in com.typesafe.sbt.SbtNativePackager.Linux := "LSIR EPFL <gsn@epfl.ch>"

debianPackageDependencies in Debian += "java7-runtime"

debianPackageRecommends in Debian ++= Seq("postgresql", "gsn-core", "nginx")

serverLoading in Debian := ServerLoader.Systemd

daemonUser in Linux := "gsn"
