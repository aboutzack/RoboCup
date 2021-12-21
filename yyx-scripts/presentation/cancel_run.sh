#!/bin/bash

. $(dirname $0)/config.sh

cd $KERNEL_DIR

./kill.sh
 
 rm $LOCKFILE_NAME

 killall -9 java