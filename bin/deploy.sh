#!/bin/bash

set -ex

latest_jar () {
  $(ls -tr target/uberjar/*standalone.jar | tail -1)
}
sudo systemctl stop timetracker.service

cd /home/timetracker/time-tracker
sudo -H -u timetracker git pull
source /home/timetracker/env.sh
sudo -H -u timetracker /home/timetracker/lein uberjar
sudo -H -u timetracker cp /home/timetracker/time-tracker.jar /home/timetracker/time-tracker-$(date +%s).jar
cp $(latest_jar) /home/timetracker/time-tracker.jar

sudo systemctl start timetracker.service
