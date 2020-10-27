#!/bin/bash

. $(dirname $0)/config.sh

CLUSTER=$1
MAP=$2
TEAM=$3
NAME=${TEAM_NAMES[$TEAM]}

SERVER=$(getServerHost $CLUSTER)

eval $(ssh $REMOTE_USER@$SERVER cat $LOCKFILE_NAME 2>/dev/null)
if [ ! -z $RUNNING_TEAM ]; then
    echo "There is already a server running on cluster $CLUSTER"
    echo "${TEAM_NAMES[$RUNNING_TEAM]} ($RUNNING_TEAM) on $RUNNING_MAP"
    exit 1
fi;

echo "Starting run for team $NAME ($TEAM) on map $MAP on cluster $CLUSTER."

if [ -f "$LOCAL_HOMEDIR/$CODEDIR/$TEAM/precompute.sh" ]; then
    echo "Starting kernel for precomputation... in $SERVER"

    ssh $REMOTE_USER@$SERVER "echo $SCRIPTDIR;$SCRIPTDIR/remoteStartKernelPrecompute.sh $MAP $TEAM" 2>&1 &

    sleep 15

    for i in 1 2 3; do
	CLIENT=$(getClientHost $CLUSTER $i)
	ssh $REMOTE_USER@$CLIENT $SCRIPTDIR/remoteStartPrecompute.sh $TEAM $SERVER $i $MAP&
    done;


    sleep $PRECOMPUTE_TIMEOUT

    echo "stopping precomputation run"
    ./cancelRun.sh $CLUSTER
fi


echo "Starting kernel..."

ssh $REMOTE_USER@$SERVER $SCRIPTDIR/remoteStartKernel.sh $MAP $TEAM&

sleep 15

#STATDIR=$LOCAL_HOMEDIR/$EVALDIR/$MAP/$TEAM
#mkdir -p $STATDIR
#cd $LOCAL_HOMEDIR/$KERNELDIR/boot
#./extract-view.sh $NAME $SERVER $STATDIR&
#cd -

#sleep 8

for i in  1 2 3; do
    CLIENT=$(getClientHost $CLUSTER $i)
    ssh $REMOTE_USER@$CLIENT $SCRIPTDIR/remoteStartAgents.sh $TEAM $SERVER $i $MAP&
done;

sleep 2

echo "Waiting fo run to finish..."

eval $(ssh $REMOTE_USER@$SERVER cat $LOCKFILE_NAME 2>/dev/null)
while [ ! -z $RUNNING_TEAM ]; do
    sleep 5
    unset RUNNING_TEAM
    eval $(ssh $REMOTE_USER@$SERVER cat $LOCKFILE_NAME 2>/dev/null)
done

./cancelRun.sh $CLUSTER

echo  "$NAME in $MAP done"
#echo "Evaluating run..."



#./evalRun.sh $CLUSTER
