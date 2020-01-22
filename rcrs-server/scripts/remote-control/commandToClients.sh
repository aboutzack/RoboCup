#!/bin/bash
. $(dirname $0)/config.sh

for HOST in $CLIENT_HOSTS; do
    ssh -t $REMOTE_USER@$HOST $* 
done


