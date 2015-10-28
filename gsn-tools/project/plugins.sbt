// The Typesafe repository 
//scalaVersion := "2.11.2"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.4.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.6.4")

addSbtPlugin("com.github.play2war" % "play2-war-plugin" % "1.4-beta1")
