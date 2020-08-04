#!/usr/bin/env bash

# Unicode symbol emojis
TASK="➡"

[[ ! $(which createdb) ]] && { echo "No postgres installation found. Ensure that postgres is installed and the bin directory is in the system PATH."; exit 1; }

[[ ! $(which lein) ]] && { echo "No leiningen installation found. Ensure that leiningen is installed and in the system PATH."; exit 1; }


echo "$TASK  Creating databases time_tracker and time_tracker_test."
createdb -U ttdev -h localhost -p 19401 -W time_tracker
createdb -U ttdev -h localhost -p 19401 -W time_tracker_test

echo "$TASK  Installing Dependencies"
lein deps


echo "$TASK  Running Migrations"
lein migrate


echo -e "All done. Start the server with \n\n lein run\n\n and visit http://localhost:8000"
