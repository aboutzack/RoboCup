#! /bin/bash

. $(dirname $0)/config.sh

function sudosshecho(){
	HOST=$1
	FILE=/etc/hosts
#	shift
	shift
	sudossh.sh $HOST echo '$*' | sudo tee --append $FILE
}
function getetchosts(){
	for c in $CLUSTERS;do
		for i in 1 2 3 4;do
			echo $IPADRRESS_PREFIX$c$i c$c$i
		done
	done
	
} 
for c in $CLUSTERS;do
	for i in 1 2 3 4;do
		sudosshecho c$c$i "127.0.0.1 localhost"
		sudosshecho c$c$i "127.0.0.1 c$c$i"	
		sudosshecho c$c$i $(getetchosts)	
	done
done
	


