-- Consultas de solo lectura
SELECT l.id FROM liquidaciones l LEFT JOIN pagos p ON p.liquidacion_id=l.id AND p.estado='APROBADO' WHERE l.estado='PAGADA' AND p.id IS NULL;
SELECT p.id FROM pagos p LEFT JOIN comprobantes c ON c.pago_id=p.id WHERE p.estado='APROBADO' AND c.id IS NULL;
SELECT p.id FROM pagos p LEFT JOIN movimientos_caja m ON m.pago_id=p.id WHERE p.estado='APROBADO' AND p.metodo='EFECTIVO' AND m.id IS NULL;
SELECT id, caja_id, operador_id, abierta_en FROM sesiones_caja WHERE estado='ABIERTA';
SELECT id, caja_id, operador_id, diferencia, cerrada_en FROM sesiones_caja WHERE estado='CERRADA' AND diferencia <> 0;
SELECT l.id FROM liquidaciones l LEFT JOIN anulaciones_liquidacion a ON a.liquidacion_id=l.id WHERE l.estado='ANULADA' AND a.id IS NULL;
