#!/bin/bash
. $(dirname $0)/config.sh
chmod -R 777  $LOCAL_HOMEDIR/$MAPDIR/
$(dirname $0)/"syncKernels.sh" $LOCAL_HOMEDIR/$MAPDIR/ $MAPDIR

