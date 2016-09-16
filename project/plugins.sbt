// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.10")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.4.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.2")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")  

//addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.6.2")  // for sbt-0.13.x or higher

//addSbtPlugin("com.typesafe.sbt" % "sbt-play-ebean" % "1.0.0")

//addSbtPlugin("com.typesafe.sbt" % "sbt-play-enhancer" % "1.1.0")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")


