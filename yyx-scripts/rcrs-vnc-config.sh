#! /bin/bash

. $(dirname $0)/config.sh

sudo apt-get install vnc4server -y
sudo apt-get install x-window-system-core -y
sudo apt-get install gdm -y
sudo apt-get install ubuntu-desktop -y
sudo apt-get install gnome-panel gnome-settings-daemon metacity nautilus gnome-terminal -y

sudo apt-get install expect -y

# /usr/bin/expect <<EOF
#             spawn vncserver :3103
#             expect {
#                 "Password:" {send "rcrsrcrs\n";exp_continue}
#                 "Verify:"   {send "rcrsrcrs\n";}
#             }       
#             expect e        
# EOF


# sudo cp -f ./$SCRIPTSDIR/xstartup .vnc/

# vncserver -kill :3103
# vncserver :3103
