// The Typesafe repository 
resolvers += "Typesafe repository" at "https://repo.maven.apache.org/maven2/"

resolvers += Resolver.typesafeRepo("releases")

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.9")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.4")

// for autoplugins
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.6")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")

//addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.6.2")  // for sbt-0.13.x or higher
//addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.14")

addSbtPlugin("com.typesafe.sbt" % "sbt-play-ebean" % "6.1.0-RC3")


// addSbtPlugin("com.typesafe.sbt" % "sbt-play-enhancer" % "1.1.0")
// addSbtPlugin("com.typesafe.sbt" % "sbt-play-enhancer" % "1.2.2")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

//addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")
addDependencyTreePlugin


