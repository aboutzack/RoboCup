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

./start-comprun.sh -m $THISMAPDIR -c $CONFIG -t $TEAM -l $KERNEL_LOGDIR &
echo "PID=$!" >> $LOCKFILE_NAME

wait


echo "RUNNING_TEAM=$TEAM in $MAP">>$SCOREFILE_NAME
echo "-----------------------------------------------------------------------------------">>$SCOREFILE_NAME
echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!I AM HERE !  I AM HERE!!  I AM  HERE !!!I AM HERE  for grep!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
cat  $KERNEL_LOGDIR/kernel.log | grep "Score" | tail -2 >>  $SCOREFILE_NAME
echo "-----------------------------------------------------------------------------------">>$SCOREFILE_NAME

echo "----------------------------------------------------------------------------------" >>$SCOREFILE_NAME

sleep 10


rm $LOCKFILE_NAME