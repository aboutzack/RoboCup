#! /bin/bash
LOCAL_USER=yyx
REMOTE_USER=rcrs
SOURCEFORGE_USER=root

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

TEAM_SHORTHANDS="APO"

declare -A TEAM_NAMES
TEAM_NAMES[APO]=Apollo-Rescue



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

    echo "c$12"
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


LOCAL_HOMEDIR=/home/$LOCAL_USER
LOCKFILE_NAME=rsl_run.lock
LOCKFILE_NAME_PRECOMP=/home/$REMOTE_USER/rsl_precomp.lock
STATFILE_NAME=rsl_last_run.stat


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


