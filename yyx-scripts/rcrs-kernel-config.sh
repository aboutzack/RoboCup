#! /bin/bash

. $(dirname $0)/config.sh

/usr/bin/expect <<EOF
            spawn ssh-keygen -t rsa
            expect {
                ":" {send "\n";exp_continue}
            }       
            expect e        
EOF