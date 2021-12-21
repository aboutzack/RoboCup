#!/bin/bash

. $(dirname $0)/config.sh

cd $ROOTDIR/CSU

/bin/sh "precompute.sh" 1 0 1 0 1 0 localhost 2>&1 | tee $CLIENT_LOGDIR

