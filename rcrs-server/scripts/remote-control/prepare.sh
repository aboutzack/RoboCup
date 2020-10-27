#!/bin/bash
. $(dirname $0)/config.sh


xterm -geometry 80x50-0+100 -T UpdateResultsAndUpload -e "autoUpdateSF.sh;" &
xterm -geometry 40x4-0+0 -T RUNS -e "autoShowRun.sh"&
