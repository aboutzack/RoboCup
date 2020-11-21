#!/bin/bash
. $(dirname $0)/config.sh

for HOST in c22;do
    #ssh $REMOTE_USER@$HOST ./$CODEDIR/test/clean.sh 
    mkdir -p ../2020-codes/RAN
    rsync -rcLv $REMOTE_USER@$HOST:$CODEDIR/b1/* ../2020-codes/RAN
done

