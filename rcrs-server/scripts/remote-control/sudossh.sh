#! /bin/bash

. $(dirname $0)/config.sh

echo "usage HOST commands"
HOST=$1
shift
ssh -t $REMOTE_USER@$HOST "$*"


