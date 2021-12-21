#!/bin/bash

. $(dirname $0)/config.sh

MAP=$1

if [ -d $MAPDIR/$MAP/config ]; then
    CONFIG=$MAPDIR/$MAP/config
else
    CONFIG=config
fi

if [ -d $MAPDIR/$MAP/map ]; then
    THISMAPDIR=$MAPDIR/$MAP/map
else
    THISMAPDIR=$MAPDIR/$MAP
fi

cd $KERNEL_DIR

./start-precompute.sh -m $THISMAPDIR -c $CONFIG -t $TEAM -l $KERNEL_LOGDIR &
echo "PID=$!" >> $LOCKFILE_NAME

wait 

