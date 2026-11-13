#!/usr/bin/env bash
# Recrea las tres bases desde cero y aplica migraciones + datos de prueba (solo local).
set -euo pipefail
cd "$(dirname "$0")"
PSQL="psql -v ON_ERROR_STOP=1 -q"
as_pg() { if [ "$(id -u)" = 0 ]; then su postgres -c "$*"; else bash -c "$*"; fi; }
as_pg "$PSQL -f $PWD/00_init_local.sql postgres"
for pair in parking:parking billing:billing identity:identity; do
  svc=${pair%%:*}; db="db_$svc"
  as_pg "psql -q -c 'DROP DATABASE IF EXISTS $db' postgres"
  as_pg "psql -q -c 'CREATE DATABASE $db' postgres"
  for f in $PWD/$svc-service/src/main/resources/db/migration/V*.sql \
           $PWD/$svc-service/src/main/resources/db/dev/V*.sql; do
    as_pg "$PSQL -f $f $db"
  done
done
echo "Bases creadas y cargadas."
