#!/bin/bash
cp=/usr/lib/jvm/java-6-openjdk/jre/../lib/tools.jar
for jarFile in $( ls lib/*jar ); do
     cp=$cp:/home/ali/gsn-0.95/$jarFile
done
java -classpath $cp gsn.Main
        
