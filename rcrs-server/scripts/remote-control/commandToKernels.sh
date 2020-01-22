#!/bin/bash
. $(dirname $0)/config.sh

for HOST in $SERVER_HOSTS; do
    ssh -t $REMOTE_USER@$HOST $* 
done

