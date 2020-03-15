#!/bin/sh


cd `dirname $0`

PWD=`pwd`
CP=`find $PWD/lib/ $PWD/jars/ -name '*.jar' ! -name '*-sources.jar' | awk -F '\n' -v ORS=':' '{print}'`

java -classpath "${CP}./build" ir.mrl.starter.MrlLauncher
