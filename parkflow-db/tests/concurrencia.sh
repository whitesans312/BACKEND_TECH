#!/usr/bin/env bash
# Pruebas con DOS sesiones reales. Dejan datos confirmados: ejecutar ./run_local.sh después.
set -u
Q() { su postgres -c "psql -X -q -t -A -d db_parking -c \"$1\"" 2>&1; }
SEL="SELECT e.codigo FROM espacios e JOIN pisos p ON p.id=e.piso_id WHERE e.habilitado AND p.habilitado AND e.tipo='CARRO' AND NOT EXISTS (SELECT 1 FROM sesiones_parqueo s WHERE s.espacio_id=e.id AND s.estado='ACTIVA') ORDER BY p.orden, e.codigo LIMIT 1 FOR UPDATE OF e SKIP LOCKED"

echo "== A) FOR UPDATE SKIP LOCKED: dos operadores simultáneos no reciben el mismo espacio"
su postgres -c "psql -X -q -t -A -d db_parking" > /tmp/a1.out 2>&1 <<SQL &
BEGIN; $SEL; SELECT pg_sleep(3); ROLLBACK;
SQL
sleep 1
su postgres -c "psql -X -q -t -A -d db_parking" > /tmp/a2.out 2>&1 <<SQL
BEGIN; $SEL; ROLLBACK;
SQL
wait
R1=$(grep -E '^P[0-9]-A' /tmp/a1.out | head -1); R2=$(grep -E '^P[0-9]-A' /tmp/a2.out | head -1)
echo "   sesión 1 -> $R1 | sesión 2 -> $R2"
[ -n "$R1" ] && [ -n "$R2" ] && [ "$R1" != "$R2" ] && echo "   OK espacios distintos" || { echo "   FALLO"; exit 1; }

echo "== B) Carrera por el MISMO espacio (índice único parcial)"
Q "INSERT INTO vehiculos (placa,tipo) VALUES ('RACE01','CARRO'),('RACE02','CARRO')" >/dev/null
INS() { echo "INSERT INTO sesiones_parqueo (numero_ticket,vehiculo_id,espacio_id,operador_entrada_id) SELECT 'PF-2026-'||lpad(nextval('seq_ticket')::text,6,'0'), v.id, e.id, '00000000-0000-0000-0000-0000000000a2' FROM vehiculos v, espacios e WHERE v.placa='$1' AND e.codigo='P2-A10'"; }
su postgres -c "psql -X -q -t -A -d db_parking" > /tmp/b1.out 2>&1 <<SQL &
BEGIN; $(INS RACE01); SELECT pg_sleep(3); COMMIT;
SQL
sleep 1
T0=$(date +%s)
su postgres -c "psql -X -q -t -A -d db_parking" > /tmp/b2.out 2>&1 <<SQL
BEGIN; $(INS RACE02); COMMIT;
SQL
T1=$(date +%s); wait
echo "   sesión 2 esperó $((T1-T0))s y respondió: $(grep -m1 -E 'ERROR|INSERT' /tmp/b2.out)"
grep -q 'uq_sesion_activa_espacio' /tmp/b2.out && [ $((T1-T0)) -ge 1 ] && echo "   OK: la segunda esperó y perdió por uq_sesion_activa_espacio" || { echo "   FALLO"; cat /tmp/b2.out; exit 1; }

echo "== C) Cierres simultáneos de la misma sesión (SELECT ... FOR UPDATE serializa)"
TK=$(Q "SELECT numero_ticket FROM sesiones_parqueo WHERE estado='ACTIVA' AND numero_ticket NOT LIKE 'PF-2026-0000%' LIMIT 1")
TK=${TK:-$(Q "SELECT numero_ticket FROM sesiones_parqueo WHERE estado='ACTIVA' ORDER BY numero_ticket LIMIT 1")}
CLOSE="UPDATE sesiones_parqueo SET estado='CERRADA', fecha_salida=now(), pago_id='f1000000-0000-0000-0000-0000000000aa', operador_cierre_id='00000000-0000-0000-0000-0000000000a2' WHERE numero_ticket='$TK'"
su postgres -c "psql -X -q -t -A -d db_parking" > /tmp/c1.out 2>&1 <<SQL &
BEGIN; SELECT estado FROM sesiones_parqueo WHERE numero_ticket='$TK' FOR UPDATE; SELECT pg_sleep(3); $CLOSE; COMMIT;
SQL
sleep 1
su postgres -c "psql -X -q -t -A -d db_parking" > /tmp/c2.out 2>&1 <<SQL
BEGIN; SELECT 'estado_visto_tras_esperar=' || estado FROM sesiones_parqueo WHERE numero_ticket='$TK' FOR UPDATE; ROLLBACK;
SQL
wait
grep -q 'estado_visto_tras_esperar=CERRADA' /tmp/c2.out && echo "   OK: la segunda vio la sesión CERRADA tras el bloqueo y no la vuelve a cerrar" || { echo "   FALLO"; cat /tmp/c2.out; exit 1; }

echo "== D) Deshabilitar un espacio MIENTRAS otra sesion abre un ticket en él (sin bloqueo de la aplicación)"
Q "INSERT INTO vehiculos (placa,tipo) VALUES ('RACE03','CARRO')" >/dev/null
su postgres -c "psql -X -q -t -A -d db_parking" > /tmp/d1.out 2>&1 <<SQL &
BEGIN; $(INS RACE03 | sed "s/P2-A10/P3-A10/"); SELECT pg_sleep(3); COMMIT;
SQL
sleep 1
su postgres -c "psql -X -q -t -A -d db_parking" > /tmp/d2.out 2>&1 <<SQL
UPDATE espacios SET habilitado=false WHERE codigo='P3-A10';
SQL
wait
grep -q 'tiene una sesion activa' /tmp/d2.out && echo "   OK: deshabilitar esperó y fue rechazado (el espacio no queda deshabilitado con vehículo dentro)" || { echo "   FALLO"; cat /tmp/d2.out; exit 1; }
echo "CONCURRENCIA: todas las pruebas pasaron"
