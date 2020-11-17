#!/bin/bash
. $(dirname $0)/config.sh
chmod 777 rcrs-vnc-config.sh
chmod 777 addRemoteHost.sh
chmod 777 rcrs-remote-config.sh

$(dirname $0)/"syncAll.sh" addRemoteHost.sh addRemoteHost.sh

$(dirname $0)/"syncKernels.sh" rcrs-vnc-config.sh rcrs-vnc-config.sh

$(dirname $0)/"syncKernels.sh" xstartup xstartup

$(dirname $0)/"syncAll.sh" rcrs-remote-config.sh  rcrs-remote-config.sh

