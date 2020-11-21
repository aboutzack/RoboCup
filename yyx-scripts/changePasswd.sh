#!/bin/bash
. $(dirname $0)/config.sh

for HOST in c11 c12 c13 c14; do
    echo "$HOST: "
    /usr/bin/expect <<EOF
            spawn ssh root@$HOST passwd rcrs
           expect {
                "password:" {send "cyuanshi\n";exp_continue}
            }        
            expect e

EOF
done


for HOST in c21 c22 c23 c24; do
    echo "$HOST: "
    /usr/bin/expect <<EOF
            spawn ssh root@$HOST passwd rcrs
            expect {
                "password:" {send "cyuanshi\n";exp_continue}
            }       
            expect e
           
EOF
done


for HOST in c31 c32 c33 c34; do
    echo "$HOST: "
    /usr/bin/expect <<EOF
            spawn ssh root@$HOST passwd rcrs
            expect {
                "password:" {send "cyuanshi\n";exp_continue}
            }       
            expect e
           
EOF
done
