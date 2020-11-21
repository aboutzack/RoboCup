. $(dirname $0)/config.sh

TEAMS=$1
if [ "$1" == "" ]; then
    TEAMS=$TEAM_SHORTHANDS
fi

for t in $TEAMS; do
	echo $LOCAL_HOMEDIR/$CODEDIR/$t/
	cp -rf $LOCAL_HOMEDIR/$CODEDIR/SAM/library/rescue $LOCAL_HOMEDIR/$CODEDIR/$t/library/
    cp -rf  $LOCAL_HOMEDIR/$CODEDIR/SAM/src/adf/sample $LOCAL_HOMEDIR/$CODEDIR/$t/src/adf/
    cp -rf $LOCAL_HOMEDIR/$CODEDIR/SAM/clean.sh $LOCAL_HOMEDIR/$CODEDIR/$t/clean.sh
    cp -rf $LOCAL_HOMEDIR/$CODEDIR/SAM/compile.sh $LOCAL_HOMEDIR/$CODEDIR/$t/compile.sh
    cp -rf $LOCAL_HOMEDIR/$CODEDIR/SAM/launch.sh $LOCAL_HOMEDIR/$CODEDIR/$t/launch.sh
    cp -rf $LOCAL_HOMEDIR/$CODEDIR/SAM/LICENSE $LOCAL_HOMEDIR/$CODEDIR/$t/LICENSE
    cp -rf $LOCAL_HOMEDIR/$CODEDIR/SAM/start.sh $LOCAL_HOMEDIR/$CODEDIR/$t/start.sh
    
done
