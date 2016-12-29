#!/bin/bash

RELEASE_TAG=$1
DEPLOY_DIR=/home/timetracker
GIT_REPO_DIR=${DEPLOY_DIR}/time-tracker
set -ex

latest_jar () {
  ls -tr target/uberjar/*standalone.jar | tail -1
}
sudo systemctl stop timetracker.service

cd ${GIT_REPO_DIR}
sudo -H -u timetracker git fetch --tags
sudo -H -u timetracker git checkout ${RELEASE_TAG}
sudo -H -u timetracker ${DEPLOY_DIR}/lein uberjar
sudo -H -u timetracker cp ${GIT_REPO_DIR}.jar ${GIT_REPO_DIR}-$(date +%s).jar
sudo -H -u timetracker cp $(latest_jar) ${GIT_REPO_DIR}.jar

sudo systemctl start timetracker.service
