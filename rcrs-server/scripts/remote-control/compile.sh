#!/bin/bash
. $(dirname $0)/config.sh

TEAMS=$1
if [ "$1" == "" ]; then
    TEAMS=$TEAM_SHORTHANDS
fi

chmod -R 777 *
for t in $TEAMS; do
	echo $LOCAL_HOMEDIR/codes/$t/
        cd $LOCAL_HOMEDIR/codes/$t/
	pwd
	./compile.sh
	cd -
done
