#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
     CREATE USER tt_test WITH PASSWORD 'tt_testpwd';
     CREATE DATABASE tt_test;
     GRANT ALL PRIVILEGES ON DATABASE tt_test TO tt_test;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "tt_test" <<-EOSQL
     ALTER SCHEMA information_schema OWNER TO tt_test;
     ALTER SCHEMA public OWNER TO tt_test;
     ALTER SCHEMA pg_catalog OWNER TO tt_test;
EOSQL
