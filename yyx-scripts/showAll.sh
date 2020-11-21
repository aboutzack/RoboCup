#!/bin/bash
. $(dirname $0)/config.sh

for HOST in $HOSTS; do
    echo "$HOST"
    ssh $REMOTE_USER@$HOST ls ./ 
    echo "codes:"
    ssh $REMOTE_USER@$HOST ls codes
    echo "maps:"
    ssh $REMOTE_USER@$HOST ls maps-2020
done