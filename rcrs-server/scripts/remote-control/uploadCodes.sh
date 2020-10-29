#!/bin/bash
. $(dirname $0)/config.sh
chmod -R 777  $LOCAL_HOMEDIR/$CODEDIR/
$(dirname $0)/"syncAll.sh" /home/yyx/$CODEDIR/ $CODEDIR


