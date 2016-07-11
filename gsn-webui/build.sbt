name := "gsn-webui"

lazy val startDjango = taskKey[Unit]("Start the django dev environment")

startDjango := {
  "./gsn-webui/start-dev.sh" !
}

NativePackagerKeys.packageSummary in com.typesafe.sbt.SbtNativePackager.Linux := "GSN Server"

NativePackagerKeys.packageDescription := "Global Sensor Networks web UI"

NativePackagerKeys.maintainer in com.typesafe.sbt.SbtNativePackager.Linux := "LSIR EPFL <gsn@epfl.ch>"

debianPackageDependencies in Debian ++= Seq("python3", "python3-pip", "python3-virtualenv")

debianPackageRecommends in Debian ++= Seq("postgresql", "gsn-core", "gsn-services")

serverLoading in Debian := ServerLoader.Systemd

daemonUser in Linux := "gsn"

mappings in Universal <+= sourceDirectory map { src => (src / "templates" / "gsn-core") -> "bin/gsn-core" }
