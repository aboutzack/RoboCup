#! /bin/bash
LOCAL_USER=rcrs
REMOTE_USER=rcrs

MAPDIR=maps-2020
KERNELDIR=rcrs-server
CODEDIR=codes
SCRIPTDIR=$KERNELDIR/scripts/remote-control
LOGDIR=logs
DISTDIR=logdist
EVALDIR=2020-evaluation

RSYNC_OPTS=-CE

CLUSTERS="2"

HOSTS="c21 c22 c23 c24"
SERVER_HOSTS="c21"
CLIENT_HOSTS="c22  c23  c24"





KERNEL_WAITING_TIME=5

PRECOMPUTE_TIMEOUT=125

DAY=rcrs-pre
YEAR=2020

TEAM_SHORTHANDS="b3"

declare -A TEAM_NAMES
TEAM_NAMES[a1]=a1-INV
TEAM_NAMES[a2]=a2-ZTRS
TEAM_NAMES[a3]=a3-APO
TEAM_NAMES[b1]=b1-RAN
TEAM_NAMES[b2]=b2-XG
TEAM_NAMES[b3]=b3-CSU
TEAM_NAMES[c1]=c1-DRE
TEAM_NAMES[c2]=c2-ZZU
TEAM_NAMES[c3]=c3-SEU




DIR=$(pwd)

declare -A CONNECT_VIEWER
CONNECT_VIEWER[1]=yes
CONNECT_VIEWER[2]=yes
CONNECT_VIEWER[3]=yes


function getServerHost() {
    echo c$11
}


function getClientHost() {

if (( $1  == -1)); then
	echo "c11"
	return
fi
    echo "c$1$(($2 + 1))"
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
LOCKFILE_NAME=/home/$REMOTE_USER/rsl_run.lock
LOCKFILE_NAME_PRECOMP=/home/$REMOTE_USER/rsl_precomp.lock
STATFILE_NAME=/home/$REMOTE_USER/rsl_last_run.stat
SCOREFILE_NAME=/home/$REMOTE_USER/Score.txt




