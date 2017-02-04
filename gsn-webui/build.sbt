name := "gsn-webui"

lazy val startDjango = taskKey[Unit]("Start the django dev environment")

startDjango := {
  "./gsn-webui/start-dev.sh" !
}

lazy val packageDjango = taskKey[Unit]("Package the django app")

packageDjango := {

  "mkdir -p ./gsn-webui/target/gsn-webui/etc/gsn-webui" !

  "mkdir -p ./gsn-webui/target/gsn-webui/etc/default" !

  "mkdir -p ./gsn-webui/target/gsn-webui/var/log/gsn-webui" !

  "mkdir -p ./gsn-webui/target/gsn-webui/usr/share/gsn-webui/bin" !

  "mkdir -p ./gsn-webui/target/gsn-webui/usr/share/gsn-webui/app" !

  "mkdir -p ./gsn-webui/target/gsn-webui/usr/share/gsn-webui/static/static" !

  "mkdir -p ./gsn-webui/target/gsn-webui/usr/share/gsn-webui/gsn/migrations" !

  "mkdir -p ./gsn-webui/target/gsn-webui/usr/lib/systemd/system" !

  "mkdir -p ./gsn-webui/target/gsn-webui/usr/bin" !

  Seq("/bin/sh", "-c", "cp ./gsn-webui/app/*.py ./gsn-webui/target/gsn-webui/usr/share/gsn-webui/app/") !

  Seq("/bin/sh", "-c", "cp -r ./gsn-webui/components/bower_components/* ./gsn-webui/target/gsn-webui/usr/share/gsn-webui/static/static/") !

  Seq("/bin/sh", "-c", "cp -r ./gsn-webui/static-files/* ./gsn-webui/target/gsn-webui/usr/share/gsn-webui/static/static/") !

  Seq("/bin/sh", "-c", "cp ./gsn-webui/gsn/*.py ./gsn-webui/target/gsn-webui/usr/share/gsn-webui/gsn/") !

  Seq("/bin/sh", "-c", "cp ./gsn-webui/gsn/migrations/*.py ./gsn-webui/target/gsn-webui/usr/share/gsn-webui/gsn/migrations") !

  "cp -r ./gsn-webui/gsn/templates ./gsn-webui/target/gsn-webui/usr/share/gsn-webui/gsn/" !

  "cp ./gsn-webui/manage.py ./gsn-webui/target/gsn-webui/usr/share/gsn-webui/" !

  "cp -r ./gsn-webui/requirements.txt ./gsn-webui/target/gsn-webui/usr/share/gsn-webui/" !

  "cp ./gsn-webui/package/templates/gsn-webui.service ./gsn-webui/target/gsn-webui/usr/lib/systemd/system/" !

  "cp ./gsn-webui/package/templates/gsn-webui ./gsn-webui/target/gsn-webui/usr/share/gsn-webui/bin/" !

  "cp ./gsn-webui/package/templates/settingsLocal.py ./gsn-webui/target/gsn-webui/usr/share/gsn-webui/app/" !

  "cp ./gsn-webui/package/templates/gsn-nginx.conf ./gsn-webui/target/gsn-webui/etc/gsn-webui/gsn-nginx.conf" !

  "cp ./gsn-webui/package/templates/etc-default ./gsn-webui/target/gsn-webui/etc/default/gsn-webui" !

  "ln -fs /usr/share/gsn-webui/app/settingsLocal.py ./gsn-webui/target/gsn-webui/etc/gsn-webui/settingsLocal.py" !

  "ln -fs /usr/share/gsn-webui/bin/gsn-webui ./gsn-webui/target/gsn-webui/usr/bin/gsn-webui" !

  "tar -czf ./gsn-webui/target/gsn-webui.tar.gz -C ./gsn-webui/target/gsn-webui ./" !

  "fpm -s tar -t deb -a all -v 2.0.1 --deb-default ./gsn-webui/package/templates/etc-default  --after-install ./gsn-webui/package/DEBIAN/postinst --after-remove ./gsn-webui/package/DEBIAN/postrm --before-remove ./gsn-webui/package/DEBIAN/prerm --deb-custom-control ./gsn-webui/package/DEBIAN/control ./gsn-webui/target/gsn-webui.tar.gz" !

}
