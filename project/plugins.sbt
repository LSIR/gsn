// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.8")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.4.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.8.0-M2")

addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.6.2")  // for sbt-0.13.x or higher

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")
