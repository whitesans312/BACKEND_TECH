-- Consultas de solo lectura
SELECT 'vehiculos_con_mas_de_una_sesion_activa' AS control, vehiculo_id::text AS referencia, count(*) AS cantidad
FROM sesiones_parqueo WHERE estado='ACTIVA' GROUP BY vehiculo_id HAVING count(*) > 1;
SELECT 'espacios_con_mas_de_una_sesion_activa' AS control, espacio_id::text AS referencia, count(*) AS cantidad
FROM sesiones_parqueo WHERE estado='ACTIVA' GROUP BY espacio_id HAVING count(*) > 1;
SELECT 'mantenimientos_activos_sin_estado_mantenimiento' AS control, m.id::text AS referencia, count(*) AS cantidad
FROM mantenimientos_espacio m JOIN espacios e ON e.id=m.espacio_id
WHERE m.estado='EN_PROCESO' AND e.estado <> 'MANTENIMIENTO' GROUP BY m.id;
SELECT 'sesiones_cerradas_sin_fecha_salida' AS control, id::text AS referencia, 1 AS cantidad
FROM sesiones_parqueo WHERE estado='CERRADA' AND hora_salida IS NULL;
