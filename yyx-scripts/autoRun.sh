#! /bin/bash

. $(dirname $0)/config.sh

# bash addLocalHost.sh

# sudo apt-get install expect -y

# bash rcrs-local-config.sh

bash uploadScript.sh

for HOST in $HOSTS;do
    ssh $REMOTE_USER@$HOST sudo bash addRemoteHost.sh
    ssh $REMOTE_USER@$HOST sudo bash rcrs-remote-config.sh
done

for HOST in $SERVER_HOSTS;do
    ssh $REMOTE_USER@$HOST sudo bash rcrs-vnc-config.sh
done


