#!/bin/bash

. $(dirname $0)/config.sh

MAP=Kobe1

./kernel_pre_run.sh $MAP 2>&1&
sleep 5

./client_pre_run.sh

sleep 5

./cancel_run.sh 


./kernel_run.sh $MAP&
sleep 15

./client_run.sh

sleep 5


./cancel_run.sh


