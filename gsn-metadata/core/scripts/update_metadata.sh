#!/bin/bash

GSN_SENSORS="url=http://montblanc.slf.ch:22002/ws/api/sensors"
SERVER="http://montblanc.slf.ch:22001/index.html"

location=${1:-$GSN_SENSORS}
server=${2:-$SERVER}

echo "location = $location"

cd ../
mvn compile exec:java -Dexec.mainClass="ch.epfl.gsn.metadata.tools.gsn.GSNImportTool" -Dexec.args="$location $server"

