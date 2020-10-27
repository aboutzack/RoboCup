#! /bin/bash

. $(dirname $0)/config.sh

MAP=$1
TEAM=$2
NAME=${TEAM_NAMES[$TEAM]}

export DISPLAY=:3.0

cd $HOME

if [ -d $MAPDIR/$MAP/config ]; then
    CONFIG=$HOME/$MAPDIR/$MAP/config
else
    CONFIG=config
fi

if [ -d $MAPDIR/$MAP/map ]; then
    THISMAPDIR=$HOME/$MAPDIR/$MAP/map
else
    THISMAPDIR=$HOME/$MAPDIR/$MAP
fi

TIME="`date +%m%d-%H%M%S`"
MAPNAME="`basename $MAP`"

KERNEL_LOGDIR=$HOME/$LOGDIR/$TIME-$NAME-$MAPNAME-precompute
mkdir -p $KERNEL_LOGDIR
cd $KERNELDIR/boot

#RESCUE_LOG=$LOGDIR/$DAY/kernel/$TIME-$NAME-$MAPNAME-precompute

echo "RUNNING_TEAM=$TEAM" >> $LOCKFILE_NAME
echo "PRECOMPUTE=yes" >> $LOCKFILE_NAME
echo "RUNNING_MAP=$MAP" >> $LOCKFILE_NAME

./start-precompute.sh -m $THISMAPDIR -c $CONFIG -t $NAME -l $KERNEL_LOGDIR &
echo "PID=$!" >> $LOCKFILE_NAME


wait

echo "RUNNING_TEAM=$TEAM" >> $STATFILE_NAME
echo "PRECOMPUTE=yes" >> $STATFILE_NAME
echo "RUNNING_MAP=$MAP" >> $STATFILE_NAME
echo "RESCUE_LOGFILE=$KERNEL_LOGDIR" >> $STATFILE_NAME

cd $KERNEL_LOGDIR
cd ..

echo "Deleting logfile...."
rm -r  ./*$TIME-$NAME-$MAPNAME*

#echo "Zipping logfile..."
#mkdir -p $HOME/$LOGDIR/$DAY/kernel/
#cp $KERNEL_LOGDIR/rescue.log $HOME/$RESCUE_LOG
#gzip $HOME/$RESCUE_LOG

#rm $LOCKFILE_NAME
echo "Precomputation done"
