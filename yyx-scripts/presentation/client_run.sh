#!/bin/bash

. $(dirname $0)/config.sh

cd $ROOTDIR/CSU

/bin/sh "start.sh" -1 -1 -1 -1 -1 -1 localhost 2>&1 | tee $CLIENT_LOGDIR