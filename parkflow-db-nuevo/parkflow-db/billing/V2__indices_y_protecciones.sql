CREATE UNIQUE INDEX uq_tarifa_vigente_concepto ON tarifas(tipo_vehiculo, concepto) WHERE activa = TRUE AND vigente_hasta IS NULL;
CREATE INDEX ix_tarifas_vigencia ON tarifas(tipo_vehiculo, concepto, vigente_desde DESC, vigente_hasta);
CREATE UNIQUE INDEX uq_liquidacion_pendiente_sesion ON liquidaciones(sesion_parqueo_id) WHERE estado = 'PENDIENTE';
CREATE INDEX ix_liquidaciones_estado_fecha ON liquidaciones(estado, creado_en DESC);
CREATE INDEX ix_liquidaciones_placa_fecha ON liquidaciones(placa, creado_en DESC);
CREATE UNIQUE INDEX uq_pago_aprobado_liquidacion ON pagos(liquidacion_id) WHERE estado = 'APROBADO';
CREATE INDEX ix_pagos_fecha_estado ON pagos(estado, creado_en DESC);
CREATE UNIQUE INDEX uq_caja_sesion_abierta ON sesiones_caja(caja_id) WHERE estado = 'ABIERTA';
CREATE UNIQUE INDEX uq_operador_sesion_caja_abierta ON sesiones_caja(operador_id) WHERE estado = 'ABIERTA';
CREATE INDEX ix_movimientos_sesion_fecha ON movimientos_caja(sesion_caja_id, creado_en DESC);
CREATE INDEX ix_detalles_liquidacion ON detalles_liquidacion(liquidacion_id);

CREATE OR REPLACE FUNCTION fn_validar_pago_liquidacion()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_total NUMERIC(12,2); v_estado VARCHAR(15); v_estado_caja VARCHAR(10);
BEGIN
 SELECT total, estado INTO v_total, v_estado FROM liquidaciones WHERE id = NEW.liquidacion_id FOR UPDATE;
 IF NOT FOUND THEN RAISE EXCEPTION 'Liquidación inexistente'; END IF;
 IF NEW.monto <> v_total THEN RAISE EXCEPTION 'El pago debe ser por el total exacto de la liquidación'; END IF;
 IF v_estado <> 'PENDIENTE' THEN RAISE EXCEPTION 'La liquidación no está pendiente'; END IF;
 IF NEW.estado = 'APROBADO' THEN
  IF NEW.sesion_caja_id IS NULL THEN RAISE EXCEPTION 'Un pago aprobado requiere caja'; END IF;
  SELECT estado INTO v_estado_caja FROM sesiones_caja WHERE id = NEW.sesion_caja_id FOR UPDATE;
  IF v_estado_caja IS DISTINCT FROM 'ABIERTA' THEN RAISE EXCEPTION 'La sesión de caja no está abierta'; END IF;
 END IF;
 RETURN NEW;
END; $$;
CREATE TRIGGER trg_validar_pago_liquidacion BEFORE INSERT OR UPDATE ON pagos FOR EACH ROW EXECUTE FUNCTION fn_validar_pago_liquidacion();

CREATE OR REPLACE FUNCTION fn_validar_movimiento_caja()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_estado VARCHAR(10);
BEGIN
 SELECT estado INTO v_estado FROM sesiones_caja WHERE id = NEW.sesion_caja_id FOR UPDATE;
 IF v_estado IS DISTINCT FROM 'ABIERTA' THEN RAISE EXCEPTION 'No se permiten movimientos en una caja cerrada'; END IF;
 IF NEW.pago_id IS NOT NULL THEN
  IF NOT EXISTS (SELECT 1 FROM pagos WHERE id = NEW.pago_id AND estado = 'APROBADO' AND sesion_caja_id = NEW.sesion_caja_id) THEN
   RAISE EXCEPTION 'El movimiento debe corresponder a un pago aprobado de esta caja';
  END IF;
 END IF;
 RETURN NEW;
END; $$;
CREATE TRIGGER trg_validar_movimiento_caja BEFORE INSERT ON movimientos_caja FOR EACH ROW EXECUTE FUNCTION fn_validar_movimiento_caja();

CREATE OR REPLACE FUNCTION fn_proteger_registro_financiero()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Los registros financieros históricos no se eliminan'; END; $$;
CREATE TRIGGER trg_no_delete_pagos BEFORE DELETE ON pagos FOR EACH ROW EXECUTE FUNCTION fn_proteger_registro_financiero();
CREATE TRIGGER trg_no_delete_comprobantes BEFORE DELETE ON comprobantes FOR EACH ROW EXECUTE FUNCTION fn_proteger_registro_financiero();
CREATE TRIGGER trg_no_delete_movimientos BEFORE DELETE ON movimientos_caja FOR EACH ROW EXECUTE FUNCTION fn_proteger_registro_financiero();
CREATE TRIGGER trg_no_delete_anulaciones_liq BEFORE DELETE ON anulaciones_liquidacion FOR EACH ROW EXECUTE FUNCTION fn_proteger_registro_financiero();
