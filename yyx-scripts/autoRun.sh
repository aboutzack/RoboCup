#! /bin/bash

. $(dirname $0)/config.sh

# bash addLocalHost.sh

# sudo apt-get install expect -y

# bash rcrs-local-config.sh

bash uploadScript.sh

for HOST in $HOSTS;do
    ssh $REMOTE_USER@$HOST sudo bash $SCRIPTSDIR/addRemoteHost.sh
    ssh $REMOTE_USER@$HOST sudo bash $SCRIPTSDIR/rcrs-remote-config.sh
done

for HOST in $SERVER_HOSTS;do
    ssh $REMOTE_USER@$HOST sudo bash $SCRIPTSDIR/rcrs-vnc-config.sh
    ssh $REMOTE_USER@$HOST bash $SCRIPTSDIR/rcrs-kernel-config.sh
    ssh $REMOTE_USER@$HOST sudo bash $SCRIPTSDIR/rcrs-local-config.sh
done


