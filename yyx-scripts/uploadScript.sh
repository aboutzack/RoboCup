#!/bin/bash
. $(dirname $0)/config.sh

chmod 777 -R  ~/$SCRIPTSDIR/


$(dirname $0)/"syncAll.sh" ~/$SCRIPTSDIR/ $SCRIPTSDIR/


