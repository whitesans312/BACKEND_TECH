\set ON_ERROR_STOP on
\set VERBOSITY terse
BEGIN;
\i tests/_helpers.sql

CREATE FUNCTION pg_temp.ins_sesion(p_placa text, p_espacio text) RETURNS uuid LANGUAGE sql AS $$
  INSERT INTO sesiones_parqueo (numero_ticket, vehiculo_id, espacio_id, operador_entrada_id)
  SELECT 'PF-2026-' || lpad(nextval('seq_ticket')::text, 6, '0'), v.id, e.id,
         '00000000-0000-0000-0000-0000000000a2'
    FROM vehiculos v, espacios e WHERE v.placa = p_placa AND e.codigo = p_espacio
  RETURNING id;
$$;

-- ---- Contadores por piso (datos de prueba) ----
SELECT pg_temp.assert_eq((SELECT string_agg(piso||':'||total_espacios||'/'||disponibles||'/'||ocupados||'/'||fuera_de_servicio, ' ')
  FROM (SELECT p.codigo AS piso, count(*) AS total_espacios,
          count(*) FILTER (WHERE e.habilitado AND p.habilitado AND s.id IS NULL) AS disponibles,
          count(*) FILTER (WHERE e.habilitado AND p.habilitado AND s.id IS NOT NULL) AS ocupados,
          count(*) FILTER (WHERE NOT (e.habilitado AND p.habilitado)) AS fuera_de_servicio
        FROM pisos p JOIN espacios e ON e.piso_id=p.id
        LEFT JOIN sesiones_parqueo s ON s.espacio_id=e.id AND s.estado='ACTIVA'
        GROUP BY p.codigo, p.orden ORDER BY p.orden) q),
  'P1:20/18/2/0 P2:20/17/3/0 P3:20/18/1/1', 'contadores por piso (total/disp/ocup/fuera)');

-- ---- RN-P1 / RN-P2 ----
SELECT pg_temp.expect($$SELECT pg_temp.ins_sesion('DEF456','P1-A04')$$, '23505', 'RN-P1 vehiculo con dos sesiones activas', 'uq_sesion_activa_vehiculo');
INSERT INTO vehiculos (placa, tipo) VALUES ('NEW111','CARRO'), ('NEW222','MOTO');
SELECT pg_temp.expect($$SELECT pg_temp.ins_sesion('NEW111','P1-A02')$$, '23505', 'RN-P2 espacio ocupado por otra sesion', 'uq_sesion_activa_espacio');

-- ---- tipo vehiculo vs espacio / espacio deshabilitado ----
SELECT pg_temp.expect($$SELECT pg_temp.ins_sesion('NEW222','P1-A10')$$, '23514', 'moto en espacio de carro');
SELECT pg_temp.expect($$SELECT pg_temp.ins_sesion('NEW111','P3-A05')$$, '23514', 'sesion en espacio fuera de servicio');

-- ---- RN-P4 ----
SELECT pg_temp.expect($$UPDATE espacios SET habilitado=false WHERE codigo='P1-A02'$$, '23514', 'RN-P4 deshabilitar espacio ocupado');
SELECT pg_temp.expect($$UPDATE pisos SET habilitado=false WHERE codigo='P1'$$, '23514', 'RN-P4 deshabilitar piso con sesiones activas');
SELECT pg_temp.expect_ok($$UPDATE espacios SET habilitado=false WHERE codigo='P1-A10'$$, 'deshabilitar espacio libre');

-- ---- coherencia por estado / ciclo de vida ----
SELECT pg_temp.expect($$UPDATE sesiones_parqueo SET estado='CERRADA', fecha_salida=now() WHERE numero_ticket='PF-2026-000003'$$,
                      '23514', 'cerrar sin pago_id ni operador', 'ck_sesiones_coherencia');
SELECT pg_temp.expect($$UPDATE sesiones_parqueo SET estado='ANULADA', operador_cierre_id='00000000-0000-0000-0000-0000000000a1' WHERE numero_ticket='PF-2026-000003'$$,
                      '23514', 'anular sin motivo', 'ck_sesiones_coherencia');
SELECT pg_temp.expect_ok($$UPDATE sesiones_parqueo SET estado='CERRADA', fecha_salida=now() + interval '1 second',
   pago_id='f1000000-0000-0000-0000-0000000000f9', operador_cierre_id='00000000-0000-0000-0000-0000000000a2'
   WHERE numero_ticket='PF-2026-000003'$$, 'cerrar sesion ACTIVA con pago_id');
SELECT pg_temp.assert_eq((SELECT count(*)::text FROM sesiones_parqueo s JOIN espacios e ON e.id=s.espacio_id
   WHERE e.codigo='P1-A02' AND s.estado='ACTIVA'), '0', 'espacio liberado al cerrar (sin sesion activa)');
SELECT pg_temp.expect($$UPDATE sesiones_parqueo SET estado='ACTIVA', fecha_salida=NULL, pago_id=NULL, operador_cierre_id=NULL
   WHERE numero_ticket='PF-2026-000003'$$, '23514', 'reabrir sesion cerrada');
SELECT pg_temp.expect($$UPDATE sesiones_parqueo SET numero_ticket='PF-2026-999999' WHERE numero_ticket='PF-2026-000004'$$,
                      '23514', 'cambiar numero_ticket');
SELECT pg_temp.expect($$UPDATE sesiones_parqueo SET pago_id='f1000000-0000-0000-0000-000000000001' WHERE numero_ticket='PF-2026-000004'$$,
                      '23514', 'ACTIVA con pago_id', 'ck_sesiones_coherencia');
SELECT pg_temp.expect($$UPDATE sesiones_parqueo SET estado='CERRADA', fecha_salida=now()+interval '1 second',
   pago_id='f1000000-0000-0000-0000-000000000001', operador_cierre_id='00000000-0000-0000-0000-0000000000a2'
   WHERE numero_ticket='PF-2026-000004'$$, '23505', 'mismo pago_id en dos sesiones', 'uq_sesion_pago');

-- ---- validaciones de formato ----
SELECT pg_temp.expect($$INSERT INTO vehiculos (placa,tipo) VALUES ('abc123','CARRO')$$, '23514', 'placa en minusculas', 'ck_vehiculos_placa');
SELECT pg_temp.expect($$INSERT INTO vehiculos (placa,tipo) VALUES ('AB-123','CARRO')$$, '23514', 'placa con guion', 'ck_vehiculos_placa');
SELECT pg_temp.expect($$INSERT INTO vehiculos (placa,tipo) VALUES ('ABC123','CARRO')$$, '23505', 'placa duplicada', 'uq_vehiculos_placa');
SELECT pg_temp.expect($$INSERT INTO espacios (piso_id,codigo,tipo) SELECT id,'P1-A01','CARRO' FROM pisos WHERE codigo='P1'$$, '23505', 'codigo de espacio duplicado');
SELECT pg_temp.expect($$INSERT INTO espacios (piso_id,codigo,tipo) SELECT id,'P1-Z99','BUS' FROM pisos WHERE codigo='P1'$$, '23514', 'tipo de espacio invalido');
SELECT pg_temp.expect($$DELETE FROM pisos WHERE codigo='P1'$$, '23503', 'FK RESTRICT: borrar piso con espacios');

-- ---- permisos: la aplicacion no puede borrar ----
SELECT pg_temp.expect($$SET LOCAL ROLE parking_app; DELETE FROM sesiones_parqueo$$, '42501', 'parking_app sin DELETE');
RESET ROLE;
SELECT pg_temp.expect_ok($$SET LOCAL ROLE parking_app; SELECT count(*) FROM sesiones_parqueo; UPDATE espacios SET updated_at=now() WHERE codigo='P2-A10'$$, 'parking_app puede SELECT/UPDATE');
RESET ROLE;

ROLLBACK;
\echo 'PARKING: todas las pruebas pasaron'
