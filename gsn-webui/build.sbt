name := "gsn-webui"

lazy val startDjango = taskKey[Unit]("Start the django dev environment")

startDjango := {
  "./gsn-webui/start-dev.sh" !
}
