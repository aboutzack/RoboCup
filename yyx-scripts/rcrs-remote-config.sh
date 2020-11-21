#! /bin/bash

. $(dirname $0)/config.sh

sudo apt update -y
sudo apt upgrade -y

sudo apt install openjdk-8-jdk-headless -y
sudo apt install gradle -y
sudo apt install expect -y

