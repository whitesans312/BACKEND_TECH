#!/usr/bin/env bash
# Reconstruye las bases y ejecuta todas las pruebas de integridad. Uso: ./run_tests.sh
set -euo pipefail
cd "$(dirname "$0")"
./run_local.sh >/dev/null
for t in parking billing identity; do
  su postgres -c "cd $PWD && psql -q -t -d db_$t -f tests/${t}_test.sql" 2>&1 | grep -E "NOTICE|ERROR|FALLO|pasaron" | sed 's/.*NOTICE:  //'
done
./tests/concurrencia.sh
./run_local.sh >/dev/null   # deja las bases limpias
