\set ON_ERROR_STOP on
\set VERBOSITY terse
BEGIN;
\i tests/_helpers.sql

-- Pago PENDIENTE válido: entrada 10:00, corte = entrada + p_mins; tarifa CARRO 60 min / 5000 / gracia 10
CREATE FUNCTION pg_temp.ins_pago(p_sesion uuid, p_key uuid, p_mins int DEFAULT 70, p_valor numeric DEFAULT NULL,
                                 p_hash text DEFAULT repeat('a',64)) RETURNS uuid LANGUAGE sql AS $$
  INSERT INTO pagos (sesion_id, numero_ticket, idempotency_key, request_hash, tarifa_id, tipo_vehiculo, placa,
                     fecha_entrada, fecha_corte, minutos_permanencia, unidad_minutos, valor_unidad, minutos_gracia,
                     moneda, unidades_cobradas, valor_total, operador_id)
  VALUES (p_sesion, 'PF-2026-000100', p_key, p_hash, 'b1000000-0000-0000-0000-000000000001', 'CARRO', 'TEST01',
          '2026-11-03 10:00:00+00', timestamptz '2026-11-03 10:00:00+00' + make_interval(mins => p_mins),
          p_mins, 60, 5000, 10, 'COP',
          ceil(greatest(0, p_mins - 10)::numeric / 60)::int,
          COALESCE(p_valor, ceil(greatest(0, p_mins - 10)::numeric / 60) * 5000),
          '00000000-0000-0000-0000-0000000000a2')
  RETURNING id;
$$;

-- ---- Fórmula de tarifa (RN-B2): 8, 30, 60, 65, 70, 71, 130, 131 minutos (130 min = 120 tras la gracia = 2 unidades exactas) ----
SELECT pg_temp.assert_eq((SELECT string_agg(m||':'||ceil(greatest(0,m-10)::numeric/60)*5000, ' ' ORDER BY m)
   FROM unnest(ARRAY[8,30,60,65,70,71,130,131]) m), '8:0 30:5000 60:5000 65:5000 70:5000 71:10000 130:10000 131:15000', 'fórmula: tabla de ejemplos');

-- ---- Idempotencia y concurrencia de liquidaciones ----
SELECT pg_temp.expect_ok($$SELECT pg_temp.ins_pago('c0000000-0000-0000-0000-000000000001','d0000000-0000-0000-0000-000000000001')$$, 'liquidar sesión nueva');
SELECT pg_temp.expect($$SELECT pg_temp.ins_pago('c0000000-0000-0000-0000-000000000002','d0000000-0000-0000-0000-000000000001')$$,
   '23505', 'misma idempotency_key repetida', 'uq_pagos_idempotency');
SELECT pg_temp.expect($$SELECT pg_temp.ins_pago('c0000000-0000-0000-0000-000000000001','d0000000-0000-0000-0000-000000000003')$$,
   '23505', 'otra clave, misma sesión con pago PENDIENTE (liquidaciones simultáneas)', 'uq_pago_sesion_vigente');
SELECT pg_temp.expect($$SELECT pg_temp.ins_pago('c0000000-0000-0000-0000-000000000001','d0000000-0000-0000-0000-000000000004')$$,
   '23505', 'tras el error, la transaccion de B debe abortarse y Billing leer el existente', 'uq_pago_sesion_vigente');

-- rechazo y nuevo intento
SELECT pg_temp.expect_ok($$UPDATE pagos SET estado='RECHAZADO', fecha_resolucion=now(), motivo_rechazo='cancelado',
   operador_resolucion_id='00000000-0000-0000-0000-0000000000a2' WHERE idempotency_key='d0000000-0000-0000-0000-000000000001'$$, 'rechazar PENDIENTE');
SELECT pg_temp.expect_ok($$SELECT pg_temp.ins_pago('c0000000-0000-0000-0000-000000000001','d0000000-0000-0000-0000-000000000005')$$, 'nuevo intento tras RECHAZADO con clave nueva');

-- ---- CHECKs de importes ----
SELECT pg_temp.expect($$SELECT pg_temp.ins_pago('c0000000-0000-0000-0000-000000000010','d0000000-0000-0000-0000-000000000010', 70, 1.00)$$,
   '23514', 'valor_total distinto de unidades x valor', 'ck_pagos_valor');
SELECT pg_temp.expect($$INSERT INTO pagos (sesion_id,numero_ticket,idempotency_key,request_hash,tarifa_id,tipo_vehiculo,placa,fecha_entrada,fecha_corte,
   minutos_permanencia,unidad_minutos,valor_unidad,minutos_gracia,moneda,unidades_cobradas,valor_total,operador_id)
   VALUES ('c0000000-0000-0000-0000-000000000011','PF-2026-000100','d0000000-0000-0000-0000-000000000011',repeat('a',64),
   'b1000000-0000-0000-0000-000000000001','CARRO','TEST01','2026-11-03 10:00+00','2026-11-03 11:10+00',69,60,5000,10,'COP',1,5000,
   '00000000-0000-0000-0000-0000000000a2')$$, '23514', 'minutos distintos de corte - entrada', 'ck_pagos_formula_minutos');
SELECT pg_temp.expect($$INSERT INTO pagos (sesion_id,numero_ticket,idempotency_key,request_hash,tarifa_id,tipo_vehiculo,placa,fecha_entrada,fecha_corte,
   minutos_permanencia,unidad_minutos,valor_unidad,minutos_gracia,moneda,unidades_cobradas,valor_total,operador_id)
   VALUES ('c0000000-0000-0000-0000-000000000012','PF-2026-000100','d0000000-0000-0000-0000-000000000012',repeat('a',64),
   'b1000000-0000-0000-0000-000000000001','CARRO','TEST01','2026-11-03 10:00+00','2026-11-03 11:10+00',70,60,5000,10,'COP',2,10000,
   '00000000-0000-0000-0000-0000000000a2')$$, '23514', 'unidades distintas de la fórmula', 'ck_pagos_formula_unidades');
SELECT pg_temp.expect($$SELECT pg_temp.ins_pago('c0000000-0000-0000-0000-000000000013','d0000000-0000-0000-0000-000000000013', 70, NULL, 'XYZ')$$,
   '23514', 'request_hash inválido', 'ck_pagos_hash');
SELECT pg_temp.expect($$INSERT INTO pagos (sesion_id,numero_ticket,idempotency_key,request_hash,tarifa_id,tipo_vehiculo,placa,fecha_entrada,fecha_corte,
   minutos_permanencia,unidad_minutos,valor_unidad,minutos_gracia,moneda,unidades_cobradas,valor_total,operador_id)
   VALUES ('c0000000-0000-0000-0000-000000000014','PF-2026-000100','d0000000-0000-0000-0000-000000000014',repeat('a',64),
   'b1000000-0000-0000-0000-000000000001','CARRO','TEST01','2026-11-03 10:00+00','2026-11-03 09:00+00',0,60,5000,10,'COP',0,0,
   '00000000-0000-0000-0000-0000000000a2')$$, '23514', 'fecha_corte anterior a la entrada', 'ck_pagos_corte');

-- ---- Coherencia por estado ----
SELECT pg_temp.expect_ok($$SELECT pg_temp.ins_pago('c0000000-0000-0000-0000-000000000020','d0000000-0000-0000-0000-000000000020')$$, 'pago PENDIENTE para pruebas de estado');
SELECT pg_temp.expect($$UPDATE pagos SET estado='APROBADO', fecha_resolucion=now(), operador_resolucion_id='00000000-0000-0000-0000-0000000000a2'
   WHERE idempotency_key='d0000000-0000-0000-0000-000000000020'$$, '23514', 'APROBADO sin método', 'ck_pagos_coherencia');
SELECT pg_temp.expect($$UPDATE pagos SET estado='APROBADO', metodo='SIN_COSTO', fecha_resolucion=now(), operador_resolucion_id='00000000-0000-0000-0000-0000000000a2'
   WHERE idempotency_key='d0000000-0000-0000-0000-000000000020'$$, '23514', 'SIN_COSTO con valor > 0', 'ck_pagos_coherencia');
SELECT pg_temp.expect($$UPDATE pagos SET estado='RECHAZADO', fecha_resolucion=now(), operador_resolucion_id='00000000-0000-0000-0000-0000000000a2'
   WHERE idempotency_key='d0000000-0000-0000-0000-000000000020'$$, '23514', 'RECHAZADO sin motivo', 'ck_pagos_coherencia');
SELECT pg_temp.expect($$UPDATE pagos SET placa='OTRA99' WHERE idempotency_key='d0000000-0000-0000-0000-000000000020'$$, '23514', 'snapshot inmutable (placa)');
SELECT pg_temp.expect($$UPDATE pagos SET request_hash=repeat('b',64) WHERE idempotency_key='d0000000-0000-0000-0000-000000000020'$$, '23514', 'request_hash inmutable');
SELECT pg_temp.expect($$UPDATE pagos SET valor_total=0, unidades_cobradas=0 WHERE idempotency_key='d0000000-0000-0000-0000-000000000020'$$, '23514', 'importe inmutable');

-- ---- Comprobantes (R2) ----
SELECT pg_temp.expect($$INSERT INTO comprobantes (pago_id, numero) SELECT id,'PF-CMP-2026-900001' FROM pagos WHERE idempotency_key='d0000000-0000-0000-0000-000000000020'$$,
   '23514', 'comprobante de pago PENDIENTE');
-- aprobar sin comprobante: la comprobación diferida falla al confirmar
SELECT pg_temp.expect($$UPDATE pagos SET estado='APROBADO', metodo='EFECTIVO', fecha_resolucion=now(), operador_resolucion_id='00000000-0000-0000-0000-0000000000a2'
   WHERE idempotency_key='d0000000-0000-0000-0000-000000000020'; SET CONSTRAINTS ALL IMMEDIATE$$, '23514', 'R2: aprobar sin comprobante falla al confirmar');
SET CONSTRAINTS ALL DEFERRED;
-- aprobar + comprobante en la misma transacción: pasa
SELECT pg_temp.expect_ok($$UPDATE pagos SET estado='APROBADO', metodo='EFECTIVO', fecha_resolucion=now(), operador_resolucion_id='00000000-0000-0000-0000-0000000000a2'
   WHERE idempotency_key='d0000000-0000-0000-0000-000000000020';
   INSERT INTO comprobantes (pago_id, numero) SELECT id, 'PF-CMP-2026-' || lpad(nextval('seq_comprobante')::text,6,'0') FROM pagos WHERE idempotency_key='d0000000-0000-0000-0000-000000000020';
   SET CONSTRAINTS ALL IMMEDIATE$$, 'R2: aprobar + comprobante en una transacción');
SET CONSTRAINTS ALL DEFERRED;
SELECT pg_temp.expect($$INSERT INTO comprobantes (pago_id, numero) SELECT id,'PF-CMP-2026-900002' FROM pagos WHERE idempotency_key='d0000000-0000-0000-0000-000000000020'$$,
   '23505', 'segundo comprobante para el mismo pago', 'uq_comprobantes_pago');
SELECT pg_temp.expect($$UPDATE pagos SET estado='RECHAZADO', fecha_resolucion=now(), motivo_rechazo='x', metodo=NULL WHERE idempotency_key='d0000000-0000-0000-0000-000000000020'$$,
   '23514', 'RN-B8 pago APROBADO es definitivo');
SELECT pg_temp.expect($$UPDATE comprobantes SET numero='PF-CMP-2026-999999'$$, '23514', 'comprobante inmutable');
SELECT pg_temp.expect($$INSERT INTO comprobantes (pago_id, numero) SELECT id,'PF-CMP-26-1' FROM pagos LIMIT 1$$, '23514', 'formato de número de comprobante');
SELECT pg_temp.assert_eq((SELECT count(*)::text FROM pagos p WHERE estado='APROBADO' AND NOT EXISTS (SELECT 1 FROM comprobantes c WHERE c.pago_id=p.id)),
   '0', 'consulta de control: aprobados sin comprobante = 0');

-- ---- Tarifas ----
SELECT pg_temp.expect($$UPDATE tarifas SET valor_unidad=9999 WHERE tipo_vehiculo='CARRO'$$, '23514', 'tarifa: valores inmutables');
SELECT pg_temp.expect($$INSERT INTO tarifas (tipo_vehiculo,unidad_minutos,valor_unidad) VALUES ('CARRO',60,6000)$$, '23505', 'dos tarifas vigentes del mismo tipo', 'uq_tarifa_vigente');
SELECT pg_temp.expect_ok($$UPDATE tarifas SET activa=false, updated_at=now() WHERE tipo_vehiculo='CARRO' AND activa$$, 'desactivar tarifa');
SELECT pg_temp.expect_ok($$INSERT INTO tarifas (tipo_vehiculo,unidad_minutos,valor_unidad,minutos_gracia) VALUES ('CARRO',60,6000,10)$$, 'crear tarifa nueva tras desactivar');
SELECT pg_temp.expect($$UPDATE tarifas SET activa=true WHERE valor_unidad=5000 AND tipo_vehiculo='CARRO'$$, '23514', 'reactivar tarifa desactivada');
SELECT pg_temp.expect($$INSERT INTO tarifas (tipo_vehiculo,unidad_minutos,valor_unidad) VALUES ('MOTO',0,100)$$, '23514', 'unidad_minutos = 0', 'ck_tarifas_unidad');

-- ---- Permisos ----
SELECT pg_temp.expect($$SET LOCAL ROLE billing_app; DELETE FROM pagos$$, '42501', 'billing_app sin DELETE');
RESET ROLE;
SELECT pg_temp.expect($$SET LOCAL ROLE billing_app; DELETE FROM comprobantes$$, '42501', 'billing_app sin DELETE en comprobantes');
RESET ROLE;

ROLLBACK;
\echo 'BILLING: todas las pruebas pasaron'
