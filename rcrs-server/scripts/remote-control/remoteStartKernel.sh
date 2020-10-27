#! /bin/bash

. $(dirname $0)/config.sh

MAP=$1
TEAM=$2
NAME=${TEAM_NAMES[$TEAM]}

export DISPLAY=:3.0
echo `pwd`
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

echo `pwd`

KERNEL_LOGDIR=$HOME/$LOGDIR/$TIME-$NAME-$MAPNAME
mkdir -p $KERNEL_LOGDIR
cd $KERNELDIR/boot

#RESCUE_LOG=$LOGDIR/$DAY/kernel/$TIME-$NAME-$MAPNAME

echo "RUNNING_TEAM=$TEAM" >> $LOCKFILE_NAME
echo "RUNNING_MAP=$MAP" >> $LOCKFILE_NAME

./start-comprun.sh -m $THISMAPDIR -c $CONFIG -t $NAME -l $KERNEL_LOGDIR &
echo "PID=$!" >> $LOCKFILE_NAME

wait

echo "RUNNING_TEAM=$TEAM" >> $STATFILE_NAME
echo "RUNNING_MAP=$MAP" >> $STATFILE_NAME
echo "LOGFILE=$KERNEL_LOGDIR" >> $STATFILE_NAME

cd $KERNEL_LOGDIR
echo "----------------------------------------------------------------------------------" >>$SCOREFILE_NAME
echo  "$NAME($TEAM) in $MAP">>$SCOREFILE_NAME
echo "-----------------------------------------------------------------------------------">>$SCOREFILE_NAME
echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!I AM HERE !  I AM HERE!!  I AM  HERE !!!I AM HERE  for grep!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
cat  ./kernel.log | grep "Score" >>  $SCOREFILE_NAME
echo "-----------------------------------------------------------------------------------">>$SCOREFILE_NAME

cd ..

echo "Deleting logfile...."
rm -r  ./*$TIME-$NAME-$MAPNAME*

#echo "Zipping logfile..."
#mkdir -p $HOME/$LOGDIR/$DAY/kernel/
#cp $KERNEL_LOGDIR/rescue.log $HOME/$RESCUE_LOG
#7za a -m0=lzma2 $HOME/$RESCUE_LOG.7z $HOME/$RESCUE_LOG
#rm -f $HOME/$RESCUE_LOG
#gzip --best $HOME/$RESCUE_LOG

rm $LOCKFILE_NAME
echo "All done"
