#!/bin/bash

if [ $# -ne 1 ]; then
	echo "usage: $0 <taxomoly-file-locatiob>"
	exit 1
fi

location="$1"

cd core/
mvn compile exec:java -Dexec.mainClass="ch.epfl.gsn.metadata.tools.taxonomy.TaxonomyImportTool" -Dexec.args="$location"

