#!/bin/bash

set -ex

DUMP_DIR=/home/timetracker/pgbackups
BUCKET=time-tracker-pg-backups
S3CONF_FILE=/home/timetracker/s3conf

latest_tar () {
  ls -tr ${DUMP_DIR}/db-dump-*.tar | tail -1
}

pg_dump timetracker -Ft > ${DUMP_DIR}/db-dump-$(date +%s).tar
s3cmd put $(latest_tar) s3://${BUCKET} -c ${S3CONF_FILE}
rm ${DUMP_DIR}/*.tar
