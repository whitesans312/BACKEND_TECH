-- SOLO LOCAL (db/dev). Tarifas con VALORES FICTICIOS: la tarifa real la crea un ADMINISTRADOR.
INSERT INTO tarifas (id, tipo_vehiculo, unidad_minutos, valor_unidad, minutos_gracia, moneda) VALUES
 ('b1000000-0000-0000-0000-000000000001', 'CARRO', 60, 5000.00, 10, 'COP'),
 ('b1000000-0000-0000-0000-000000000002', 'MOTO',  60, 2500.00, 10, 'COP');

-- Pago 1: 145 min, gracia 10 -> ceil(135/60)=3 unidades x 5000 = 15000 (EFECTIVO)
BEGIN;
INSERT INTO pagos (id, sesion_id, numero_ticket, idempotency_key, request_hash, estado, metodo,
                   tarifa_id, tipo_vehiculo, placa, fecha_entrada, fecha_corte, minutos_permanencia,
                   unidad_minutos, valor_unidad, minutos_gracia, moneda, unidades_cobradas, valor_total,
                   operador_id, operador_resolucion_id, fecha_resolucion)
VALUES ('f1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001', 'PF-2026-000001',
        'a1000000-0000-0000-0000-000000000001',
        encode(sha256(convert_to('e1000000-0000-0000-0000-000000000001|PF-2026-000001|ABC123|CARRO|2026-11-02T13:00:00Z','UTF8')),'hex'),
        'PENDIENTE', NULL,
        'b1000000-0000-0000-0000-000000000001', 'CARRO', 'ABC123', '2026-11-02 13:00:00+00', '2026-11-02 15:25:00+00', 145,
        60, 5000.00, 10, 'COP', 3, 15000.00,
        '00000000-0000-0000-0000-0000000000a2', NULL, NULL);
UPDATE pagos SET estado = 'APROBADO', metodo = 'EFECTIVO', fecha_resolucion = '2026-11-02 15:27:00+00',
                 operador_resolucion_id = '00000000-0000-0000-0000-0000000000a2', updated_at = now()
 WHERE id = 'f1000000-0000-0000-0000-000000000001';
INSERT INTO comprobantes (pago_id, numero)
VALUES ('f1000000-0000-0000-0000-000000000001', 'PF-CMP-2026-' || lpad(nextval('seq_comprobante')::text, 6, '0'));
COMMIT;

-- Pago 2: 8 min dentro de la gracia -> 0 unidades, SIN_COSTO
BEGIN;
INSERT INTO pagos (id, sesion_id, numero_ticket, idempotency_key, request_hash, estado, metodo,
                   tarifa_id, tipo_vehiculo, placa, fecha_entrada, fecha_corte, minutos_permanencia,
                   unidad_minutos, valor_unidad, minutos_gracia, moneda, unidades_cobradas, valor_total,
                   operador_id)
VALUES ('f1000000-0000-0000-0000-000000000002', 'e1000000-0000-0000-0000-000000000002', 'PF-2026-000002',
        'a1000000-0000-0000-0000-000000000002',
        encode(sha256(convert_to('e1000000-0000-0000-0000-000000000002|PF-2026-000002|XYZ98|MOTO|2026-11-02T09:00:00Z','UTF8')),'hex'),
        'PENDIENTE', NULL,
        'b1000000-0000-0000-0000-000000000002', 'MOTO', 'XYZ98', '2026-11-02 09:00:00+00', '2026-11-02 09:08:00+00', 8,
        60, 2500.00, 10, 'COP', 0, 0.00,
        '00000000-0000-0000-0000-0000000000a2');
UPDATE pagos SET estado = 'APROBADO', metodo = 'SIN_COSTO', fecha_resolucion = '2026-11-02 09:08:30+00',
                 operador_resolucion_id = '00000000-0000-0000-0000-0000000000a2', updated_at = now()
 WHERE id = 'f1000000-0000-0000-0000-000000000002';
INSERT INTO comprobantes (pago_id, numero)
VALUES ('f1000000-0000-0000-0000-000000000002', 'PF-CMP-2026-' || lpad(nextval('seq_comprobante')::text, 6, '0'));
COMMIT;
