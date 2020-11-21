#!/bin/bash

. $(dirname $0)/config.sh

CLUSTER=2

SERVER=$(getServerHost $CLUSTER)


    echo "killing kernel..."

    ssh $REMOTE_USER@$SERVER  $KERNELDIR/boot/kill.sh
    ssh $REMOTE_USER@$SERVER rm $LOCKFILE_NAME


echo "killing clients"

for i in 1 2 3; do
    CLIENT=$(getClientHost $CLUSTER $i)
    ssh $REMOTE_USER@$CLIENT "killall -9 java"
done;
