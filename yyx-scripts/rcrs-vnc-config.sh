#! /bin/bash


sudo apt-get install vnc4server -y
sudo apt-get install x-window-system-core -y
sudo apt-get install gdm -y
sudo apt-get install ubuntu-desktop -y
sudo apt-get install gnome-panel gnome-settings-daemon matacity nautilus gonme-terminal -y

sudo cp -f xstartup .vnc/
