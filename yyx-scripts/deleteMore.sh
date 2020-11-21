#!/bin/bash
. $(dirname $0)/config.sh

for HOST in $SERVER_HOSTS; do
    echo "$HOST"
    ssh $REMOTE_USER@$HOST $KERNELDIR/remote-control/cancelRun.sh
done


for HOST in $HOSTS; do
    echo "$HOST: "
    echo "ssh $REMOTE_USER@$HOST ls ~"
    #ssh $REMOTE_USER@$HOST rm -r rcrs-server/scripts/remote-control/ maps-2020/
    # maps-2020/* 
    ssh $REMOTE_USER@$HOST rm -r codes/rcrs-adf-sample logs/   2020-evaluation rsl_last_run.stat Score.txt .local/share/Trash 
    ssh $REMOTE_USER@$HOST ls /home/rcrs
    ssh $REMOTE_USER@$HOST ls /home/rcrs/codes/
done