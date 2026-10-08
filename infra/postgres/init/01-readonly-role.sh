#!/bin/bash
# Runs once, on first start of an empty data volume.
# Grafana (and later the agent) connect as `readonly`: they can read, never write.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-SQL
    create role readonly login password '${READONLY_PASSWORD}';
    grant connect on database "${POSTGRES_DB}" to readonly;
    grant usage on schema public to readonly;
    -- Tables/views are created later by Flyway (as $POSTGRES_USER), so grant ahead of time.
    alter default privileges for role "${POSTGRES_USER}" in schema public grant select on tables to readonly;
SQL
