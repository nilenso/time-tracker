#!/usr/bin/env bash

latest_jar () {
    ls -tr target/uberjar/*standalone.jar | tail -1
}

# get last commit hash prepended with @ (i.e. @8a323d0)
function parse_git_hash() {
    git rev-parse --short HEAD 2> /dev/null | sed "s/\(.*\)/@\1/"
}

now=$(date)
USERNAME=enso
HOST=time.nilenso.com
LOGS=/home/enso/logs/time-tracker
DEPLOY_LOG=$LOGS/deploys.log
MSG="[$now] Deployed commit $(parse_git_hash)"

# Unicode symbol emojis
TASK="➡"
ROCKON="🤘"

SCRIPT="
sudo systemctl stop timetracker.service
sudo systemctl start timetracker.service

mkdir -p $LOGS
touch $DEPLOY_LOG;
echo $MSG >> $DEPLOY_LOG
"


echo "$TASK  Cleaning previous build"
lein clean


echo "$TASK  Generating jar"
lein uberjar


echo "$TASK  Deploying artifacts to server"
mv $(latest_jar) target/uberjar/time-tracker.jar
scp -r target/uberjar/time-tracker.jar ${USERNAME}@${HOST}:time-tracker/
ssh -l ${USERNAME} -t ${HOST} "${SCRIPT}"

echo "=>  Deploy log written to $DEPLOY_LOG"

echo "$TASK  Deployment Complete! $ROCKON"
