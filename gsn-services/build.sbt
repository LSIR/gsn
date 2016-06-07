name := "gsn-services"

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

val buildSettings = Defaults.defaultSettings ++ Seq(
   javaOptions += "-Xmx128m",
   javaOptions += "-Xms64m"
)

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
  "gsn" % "gsn-tools" % "2.0.0-SNAPSHOT",
  "com.typesafe.play" %% "play-json" % "2.3.10",
  "com.typesafe.akka" %% "akka-actor" % "2.3.14"
  )
