#!/bin/bash
. $(dirname $0)/config.sh

TEAMS=$1
if [ "$1" == "" ]; then
    TEAMS=$TEAM_SHORTHANDS
fi

chmod -R 777 *
for t in $TEAMS; do
	echo /home/$LOCAL_USER/$CODEDIR/$t/
        cd /home/$LOCAL_USER/$CODEDIR/$t/
	pwd
	./compile.sh
	cd -
done
