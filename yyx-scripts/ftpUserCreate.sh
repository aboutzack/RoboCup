#!/bin/bash
. $(dirname $0)/config.sh

for HOST in $SERVER_HOSTS; do
        echo $HOST

    if [ "$HOST" == 'c11' ]; then 
            CHAR="a"
    else
        if [ $HOST == "c21" ];then 
            CHAR="b"
        else
            CHAR="c"
        fi
    fi 
 
    ssh $REMOTE_USER@$HOST sudo mkdir -p /etc/vsftpd/vsftpd_user_conf

    for NUM in 1 2 3; do
    
    echo $CHAR$NUM
    
    # ssh $REMOTE_USER@$HOST sudo useradd  -d /home/$REMOTE_USER/$CODEDIR/a1/ -s /sbin/nologin $CHAR$NUM
    # ssh root@$HOST usermod -g rcrs $TEAM
    ssh $REMOTE_USER@$HOST mkdir -p /home/$REMOTE_USER/$CODEDIR/$CHAR$NUM/
    ssh $REMOTE_USER@$HOST sudo chmod 777 -R /home/$REMOTE_USER/$CODEDIR/$CHAR$NUM/
    
    # FILE=/home/$REMOTE_USER/$CODEDIR/$CHAR$NUM/
    #ssh root@$HOST  echo "local_root=$FILE" > /etc/vsftpd/vsftpd_user_conf/$CHAR$NUM

    # /usr/bin/expect <<EOF
    #         spawn ssh $REMOTE_USER@$HOST sudo passwd $CHAR$NUM
    #        expect {
    #             "password:" {send "$CHAR$NUM\n";exp_continue}
    #         }        
    #         expect e

#EOF
    done


    ssh $REMOTE_USER@$HOST sudo service vsftpd restart
done