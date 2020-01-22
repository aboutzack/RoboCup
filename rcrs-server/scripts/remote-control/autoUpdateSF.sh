



#! /bin/bash

. $(dirname $0)/config.sh


while [ 1 ];do
	MAPS=`cd ~/maps/;ls -d */`
	for MAP in $MAPS; do
	    MAP=${MAP%/}
		MAP_EVALDIR=$EVALDIR/$MAP
	if [[ -d $MAP_EVALDIR ]];then
		echo $MAP
		mapSummary.sh $MAP
	fi

	done	

	cd $LOCAL_HOMEDIR/$EVALDIR
	make_overview.py >index.html
	cd - >>/dev/null
	gatherFromClients.sh ~/logs/ ~/evaluation/agent-logs/

	#rsync -rcLv /home/$LOCAL_USER/evaluation/ $LOCAL_USER@10.10.10.101:/var/www/html/results/
	rsync -avrlce ssh /home/rsim/evaluation/ alim1369@frs.sf.net:/home/project-web/roborescue/htdocs/data/2019-RSL/agent/results

		
	
	

	
	echo "Uploading Results  Finished."
	sleep 30
done
