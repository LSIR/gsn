name := "gsn-services"

version := "0.0.2"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  jdbc,
  ws,
  cache,
  "com.h2database" % "h2" % "1.4.181",
  "mysql" % "mysql-connector-java" % "5.1.6"
)

//routesImport += "controllers.gsn.api.Binders._"