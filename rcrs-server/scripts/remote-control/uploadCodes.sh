#!/bin/bash
. $(dirname $0)/config.sh
chmod -R 777  $LOCAL_HOMEDIR/$CODEDIR/
$(dirname $0)/"syncClients.sh" $LOCAL_HOMEDIR/$CODEDIR/ $CODEDIR


