name := "gsn-webui"

lazy val setupDjango = taskKey[Unit]("Setup the Django environment")

setupDjango := {
  "./gsn-webui/configure-dev.sh" !
}

lazy val startDjango = taskKey[Unit]("Start the django dev environment")

startDjango := {
  "./gsn-webui/start-dev.sh" !
}