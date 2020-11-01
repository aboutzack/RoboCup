#! /bin/bash
#  当本地用户和远程用户都是root用户时
LOCAL_USER=root
REMOTE_USER=root

# 当本地用户或者远程用户为普通用户时
#LOCAL_USER=home/yyx
#REMOTE_USER=home/yyx


MAPDIR=maps
KERNELDIR=rcrs-server
CODEDIR=code
SCRIPTDIR=$KERNELDIR/scripts/remote-control
LOGDIR=logs
DISTDIR=logdist
EVALDIR=evaluation

RSYNC_OPTS=-CE

CLUSTERS="1"

HOSTS="c11 c12 c13 c14"
SERVER_HOSTS="c11"
CLIENT_HOSTS="c12  c13  c14"



# HOSTS=localhost
# SERVER_HOSTS=localhost
# CLIENT_HOSTS=localhost

KERNEL_WAITING_TIME=5

PRECOMPUTE_TIMEOUT=125

DAY=csu-test
YEAR=2020

TEAM_SHORTHANDS="CSU APO"
#TEAM_SHORTHANDS="APO"

declare -A TEAM_NAMES
TEAM_NAMES[APO]=Apollo-Rescue
TEAM_NAMES[CSU]=CSU_YunLu



DIR=$(pwd)

declare -A CONNECT_VIEWER
CONNECT_VIEWER[1]=yes
CONNECT_VIEWER[2]=yes
CONNECT_VIEWER[3]=yes


#return hostname of the kernel server of the given cluster
function getServerHost() {
    echo c$11
    # echo 10.10.10.$11
    # echo localhost
}

#return hostnames of the client servers of the given cluster
function getClientHost() {

if (( $1  == -1)); then
	echo "c11"
	return
fi
    echo "c$1$(($2 + 1))"
    # echo localhost
}

function getAllServerHosts() {
	for C in $SERVER_HOSTS; do
	    echo -n "$C"
	done
}

function getAllClientHosts() {
	for C in $CLIENT_HOSTS; do
	    echo -n "$C"
	done
}


LOCAL_HOMEDIR=/$LOCAL_USER
LOCKFILE_NAME=/$REMOTE_USER/rsl_run.lock
LOCKFILE_NAME_PRECOMP=/$REMOTE_USER/rsl_precomp.lock
STATFILE_NAME=/$REMOTE_USER/rsl_last_run.stat
SCOREFILE_NAME=/$REMOTE_USER/Score.txt

#SERVER_HOSTS=$(getAllServerHosts);
#CLIENT_HOSTS=$(getAllClientHosts);
#HOSTS="$SERVER_HOSTS $CLIENT_HOSTS"


#for HOST in $SERVER_HOSTS; do
#	echo Kernel=$HOST
#done
#for HOST in $CLIENT_HOSTS; do
#	echo Kernel=$HOST
#done
#for HOST in $HOSTS; do
#	echo All=$HOST
#done


