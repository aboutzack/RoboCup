#!/bin/bash

echo "Uploading codes"
uploadCodes.sh

echo "Uploading kernel"
syncKernels.sh ~/roborescue/ ~/roborescue/
echo "Uploading maps"
syncKernels.sh ~/maps/ ~/maps/
echo "Uploading scripts"
syncAll.sh ~/scripts/ ~/scripts/
