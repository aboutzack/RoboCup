#! /bin/bash

. $(dirname $0)/config.sh

    for HOST in $HOSTS;do
/usr/bin/expect <<EOF
            spawn sudo ssh-copy-id  rcrs@$HOST 
            expect {
                "(yes/no)?" {send "yes\n";exp_continue}
                "password:" {send "rcrs\n";}
            }       
            expect e
            spawn sudo ssh-copy-id  root@$HOST 
            expect {
                "(yes/no)?" {send "yes\n";exp_continue}
                "password:" {send "CSU_yunlu\n";}
            }
            expect e
EOF
    done