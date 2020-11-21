#!/bin/bash
. $(dirname $0)/config.sh

for HOST in $SERVER_HOSTS; do
    ssh root@$HOST  chmod 777 -R /home/rcrs/codes
done