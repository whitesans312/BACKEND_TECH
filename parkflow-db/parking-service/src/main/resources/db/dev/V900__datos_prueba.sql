-- SOLO LOCAL (db/dev). Operadores: a1 = admin, a2 = operador (UUID fijos de db_identity dev).
INSERT INTO pisos (codigo, nombre, orden) VALUES ('P1','Piso 1',1), ('P2','Piso 2',2), ('P3','Piso 3',3);

-- 3 pisos x 20 espacios: A01..A18 CARRO, A19..A20 MOTO
INSERT INTO espacios (piso_id, codigo, tipo)
SELECT p.id, p.codigo || '-A' || lpad(i::text, 2, '0'), CASE WHEN i >= 19 THEN 'MOTO' ELSE 'CARRO' END
  FROM pisos p CROSS JOIN generate_series(1, 20) AS i;

-- Un espacio fuera de servicio (sin sesión activa)
UPDATE espacios SET habilitado = false, updated_at = now() WHERE codigo = 'P3-A05';

INSERT INTO vehiculos (placa, tipo) VALUES
 ('ABC123','CARRO'), ('DEF456','CARRO'), ('GHI789','CARRO'), ('JKL012','CARRO'),
 ('MNO345','CARRO'), ('PQR678','CARRO'), ('XYZ98','MOTO'),  ('MTR55','MOTO');

-- Sesiones CERRADAS (sus pagos viven en db_billing dev, con los mismos UUID)
INSERT INTO sesiones_parqueo (id, numero_ticket, vehiculo_id, espacio_id, estado, fecha_entrada,
                              fecha_salida, pago_id, operador_entrada_id, operador_cierre_id)
SELECT 'e1000000-0000-0000-0000-000000000001', 'PF-2026-000001', v.id, e.id, 'CERRADA',
       '2026-11-02 13:00:00+00', '2026-11-02 15:31:00+00', 'f1000000-0000-0000-0000-000000000001',
       '00000000-0000-0000-0000-0000000000a2', '00000000-0000-0000-0000-0000000000a2'
  FROM vehiculos v, espacios e WHERE v.placa = 'ABC123' AND e.codigo = 'P1-A01';
INSERT INTO sesiones_parqueo (id, numero_ticket, vehiculo_id, espacio_id, estado, fecha_entrada,
                              fecha_salida, pago_id, operador_entrada_id, operador_cierre_id)
SELECT 'e1000000-0000-0000-0000-000000000002', 'PF-2026-000002', v.id, e.id, 'CERRADA',
       '2026-11-02 09:00:00+00', '2026-11-02 09:09:00+00', 'f1000000-0000-0000-0000-000000000002',
       '00000000-0000-0000-0000-0000000000a2', '00000000-0000-0000-0000-0000000000a2'
  FROM vehiculos v, espacios e WHERE v.placa = 'XYZ98' AND e.codigo = 'P1-A19';

SELECT setval('seq_ticket', 2);

-- Sesiones ACTIVAS (tickets por secuencia)
INSERT INTO sesiones_parqueo (numero_ticket, vehiculo_id, espacio_id, fecha_entrada, operador_entrada_id)
SELECT 'PF-2026-' || lpad(nextval('seq_ticket')::text, 6, '0'), v.id, e.id,
       now() - make_interval(mins => m.mins), '00000000-0000-0000-0000-0000000000a2'
  FROM (VALUES ('DEF456','P1-A02',95), ('GHI789','P1-A03',40), ('JKL012','P2-A01',20),
               ('MNO345','P2-A02',7),  ('PQR678','P3-A01',180), ('MTR55','P2-A19',55)) AS m(placa, cod, mins)
  JOIN vehiculos v ON v.placa = m.placa
  JOIN espacios e  ON e.codigo = m.cod;
