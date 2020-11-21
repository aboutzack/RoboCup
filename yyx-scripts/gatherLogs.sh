#!/bin/bash
. $(dirname $0)/config.sh

mkdir -p ../2020-logs

mkdir -p ../2020-evaluation

mkdir -p ../2020-Score

for HOST in $SERVER_HOSTS;do  
    
    scp -r $REMOTE_USER@$HOST:logs/* ../2020-logs

    
    scp -r $REMOTE_USER@$HOST:logs/2020-evaluation ../2020-evaluation

    scp -r $REMOTE_USER@$HOST:Score.txt     ../

    cat ../Score.txt >> ../2020-Score/Score.txt    

done

rm ../Score.txt