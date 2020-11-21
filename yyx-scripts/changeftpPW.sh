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
 
   

    for NUM in 1 2 3; do
    
    echo $CHAR$NUM
    
   
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