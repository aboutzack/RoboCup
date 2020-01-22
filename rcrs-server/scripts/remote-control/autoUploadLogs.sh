#! /bin/bash

MAPS=`cd ~/maps/;ls -d */`
	for MAP in $MAPS; do
	    MAP=${MAP%/}
		echo $MAP
		uploadLogs.sh $MAP 
	done	
