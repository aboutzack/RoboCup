#!/bin/bash
. $(dirname $0)/config.sh
cd $SCRIPTDIR
find $HOME/$EVALDIR/agent-logs/ -type d -exec cp index.php {} \;

