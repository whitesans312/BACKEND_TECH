CREATE UNIQUE INDEX uq_tarifa_vigente_abierta ON tarifas(tipo_vehiculo, concepto) WHERE activa AND vigente_hasta IS NULL;
CREATE INDEX ix_tarifas_historial ON tarifas(tipo_vehiculo, concepto, vigente_desde DESC);
CREATE UNIQUE INDEX uq_cargo_vigente_caso ON cargos_abandono(caso_abandono_id) WHERE estado IN ('AUTORIZADO','LIQUIDADO');
CREATE UNIQUE INDEX uq_liquidacion_pendiente_sesion ON liquidaciones(sesion_parqueo_id) WHERE sesion_parqueo_id IS NOT NULL AND estado = 'PENDIENTE';
CREATE UNIQUE INDEX uq_liquidacion_pendiente_cargo ON liquidaciones(cargo_abandono_id) WHERE cargo_abandono_id IS NOT NULL AND estado = 'PENDIENTE';
CREATE INDEX ix_liquidaciones_estado_fecha ON liquidaciones(estado, creado_en DESC);
CREATE INDEX ix_liquidaciones_placa_fecha ON liquidaciones(placa, creado_en DESC) WHERE placa IS NOT NULL;
CREATE UNIQUE INDEX uq_pago_aprobado_liquidacion ON pagos(liquidacion_id) WHERE estado = 'APROBADO';
CREATE UNIQUE INDEX uq_pago_pendiente_liquidacion ON pagos(liquidacion_id) WHERE estado = 'PENDIENTE';
CREATE INDEX ix_pagos_estado_fecha ON pagos(estado, creado_en DESC);
CREATE INDEX ix_movimientos_caja_fecha ON movimientos_caja(sesion_caja_id, creado_en DESC);
CREATE INDEX ix_detalles_liquidacion ON detalles_liquidacion(liquidacion_id);
CREATE INDEX ix_cargo_eventos_fecha ON eventos_cargo_abandono(cargo_abandono_id, creado_en DESC);

CREATE FUNCTION fn_proteger_vigencia_tarifa() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_tipo VARCHAR(10); v_concepto VARCHAR(25);
BEGIN
    v_tipo := COALESCE(NEW.tipo_vehiculo, OLD.tipo_vehiculo);
    v_concepto := COALESCE(NEW.concepto, OLD.concepto);
    PERFORM pg_advisory_xact_lock(hashtextextended(v_tipo || ':' || v_concepto, 0));
    IF TG_OP <> 'DELETE' AND NEW.activa THEN
        IF EXISTS (
            SELECT 1 FROM tarifas t
            WHERE t.id <> NEW.id AND t.tipo_vehiculo = NEW.tipo_vehiculo
              AND t.concepto = NEW.concepto AND t.activa
              AND tstzrange(t.vigente_desde, COALESCE(t.vigente_hasta, 'infinity'::timestamptz), '[)')
                  && tstzrange(NEW.vigente_desde, COALESCE(NEW.vigente_hasta, 'infinity'::timestamptz), '[)')
        ) THEN RAISE EXCEPTION 'Vigencias tarifarias activas no pueden solaparse'; END IF;
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_no_solapar_tarifas BEFORE INSERT OR UPDATE OR DELETE ON tarifas
FOR EACH ROW EXECUTE FUNCTION fn_proteger_vigencia_tarifa();

CREATE FUNCTION fn_validar_cargo_abandono() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.estado IS DISTINCT FROM OLD.estado THEN
        IF NOT ((OLD.estado = 'AUTORIZADO' AND NEW.estado IN ('REVOCADO','LIQUIDADO'))
             OR (OLD.estado = 'LIQUIDADO' AND NEW.estado = 'PAGADO')) THEN
            RAISE EXCEPTION 'Transición de cargo no permitida: % -> %', OLD.estado, NEW.estado;
        END IF;
    END IF;
    IF NEW.estado = 'PAGADO' AND NOT EXISTS (
        SELECT 1 FROM liquidaciones l JOIN pagos p ON p.liquidacion_id = l.id
        WHERE l.cargo_abandono_id = NEW.id AND l.estado = 'PAGADA' AND p.estado = 'APROBADO'
    ) THEN RAISE EXCEPTION 'Cargo solo puede marcarse PAGADO tras pago aprobado'; END IF;
    IF NEW.estado = 'LIQUIDADO' AND NOT EXISTS (
        SELECT 1 FROM liquidaciones l WHERE l.cargo_abandono_id = NEW.id AND l.estado = 'PENDIENTE'
    ) THEN RAISE EXCEPTION 'Cargo LIQUIDADO requiere liquidación pendiente'; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_transicion_cargo_abandono BEFORE INSERT OR UPDATE ON cargos_abandono
FOR EACH ROW EXECUTE FUNCTION fn_validar_cargo_abandono();

CREATE FUNCTION fn_validar_pago() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_total NUMERIC(12,2); v_estado_liq VARCHAR(15); v_estado_caja VARCHAR(10);
BEGIN
    SELECT total, estado INTO v_total, v_estado_liq FROM liquidaciones WHERE id = NEW.liquidacion_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Liquidación inexistente'; END IF;
    IF NEW.monto <> v_total THEN RAISE EXCEPTION 'Pago debe ser del total exacto'; END IF;
    IF NEW.estado = 'PENDIENTE' AND v_estado_liq <> 'PENDIENTE' THEN RAISE EXCEPTION 'Liquidación no está pendiente'; END IF;
    IF NEW.estado = 'APROBADO' THEN
        IF v_estado_liq <> 'PENDIENTE' THEN RAISE EXCEPTION 'Liquidación no está pendiente'; END IF;
        SELECT estado INTO v_estado_caja FROM sesiones_caja WHERE id = NEW.sesion_caja_id FOR UPDATE;
        IF v_estado_caja IS DISTINCT FROM 'ABIERTA' THEN RAISE EXCEPTION 'Caja no está abierta'; END IF;
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.estado = 'APROBADO' AND NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'Pago aprobado es inmutable';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.estado = 'RECHAZADO' AND NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'Pago rechazado es terminal';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_validar_pago BEFORE INSERT OR UPDATE ON pagos
FOR EACH ROW EXECUTE FUNCTION fn_validar_pago();

CREATE FUNCTION fn_validar_movimiento_caja() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_estado VARCHAR(10); v_delta NUMERIC(12,2);
BEGIN
    SELECT estado INTO v_estado FROM sesiones_caja WHERE id = NEW.sesion_caja_id FOR UPDATE;
    IF v_estado IS DISTINCT FROM 'ABIERTA' THEN RAISE EXCEPTION 'Caja no está abierta'; END IF;
    IF NEW.pago_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM pagos p WHERE p.id = NEW.pago_id AND p.estado = 'APROBADO'
          AND p.liquidacion_id = NEW.liquidacion_id AND p.sesion_caja_id = NEW.sesion_caja_id AND p.monto = NEW.monto
    ) THEN RAISE EXCEPTION 'Movimiento debe coincidir con el pago aprobado'; END IF;
    v_delta := CASE WHEN NEW.tipo_movimiento = 'INGRESO' THEN NEW.monto ELSE -NEW.monto END;
    UPDATE sesiones_caja SET saldo_esperado = saldo_esperado + v_delta, actualizado_en = now()
    WHERE id = NEW.sesion_caja_id AND saldo_esperado + v_delta >= 0;
    IF NOT FOUND THEN RAISE EXCEPTION 'Movimiento dejaría saldo esperado negativo'; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_validar_movimiento_caja BEFORE INSERT ON movimientos_caja
FOR EACH ROW EXECUTE FUNCTION fn_validar_movimiento_caja();

CREATE FUNCTION fn_validar_transicion_liquidacion() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.estado <> 'PENDIENTE' AND NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'Liquidación terminal es inmutable';
    END IF;
    IF OLD.estado IN ('PAGADA','GRATUITA','CANCELADA','ANULADA') AND NEW.estado IS DISTINCT FROM OLD.estado THEN
        RAISE EXCEPTION 'Estado terminal de liquidación es inmutable';
    END IF;
    IF NEW.estado = 'PAGADA' AND NOT EXISTS (SELECT 1 FROM pagos WHERE liquidacion_id = NEW.id AND estado = 'APROBADO') THEN
        RAISE EXCEPTION 'Liquidación no puede estar PAGADA sin pago aprobado';
    END IF;
    IF NEW.estado = 'GRATUITA' AND EXISTS (SELECT 1 FROM pagos WHERE liquidacion_id = NEW.id) THEN
        RAISE EXCEPTION 'Liquidación gratuita no puede tener intentos de pago';
    END IF;
    IF NEW.estado IN ('CANCELADA','ANULADA','GRATUITA') AND EXISTS (SELECT 1 FROM pagos WHERE liquidacion_id = NEW.id AND estado IN ('PENDIENTE','APROBADO')) THEN
        RAISE EXCEPTION 'Resolver intentos de pago antes de cerrar liquidación';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_transicion_liquidacion BEFORE UPDATE ON liquidaciones
FOR EACH ROW EXECUTE FUNCTION fn_validar_transicion_liquidacion();

CREATE FUNCTION fn_validar_comprobante() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_estado VARCHAR(15); v_cargo UUID;
BEGIN
    SELECT estado, cargo_abandono_id INTO v_estado, v_cargo FROM liquidaciones WHERE id = NEW.liquidacion_id;
    IF NEW.tipo = 'SALIDA_GRATUITA' AND (v_estado <> 'GRATUITA' OR NEW.total <> 0 OR v_cargo IS NOT NULL) THEN
        RAISE EXCEPTION 'Constancia gratuita requiere liquidación gratuita de sesión';
    END IF;
    IF NEW.tipo = 'PAGO_EFECTIVO' AND (v_estado <> 'PAGADA' OR NEW.total <= 0) THEN
        RAISE EXCEPTION 'Comprobante de pago requiere liquidación pagada';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_validar_comprobante BEFORE INSERT ON comprobantes
FOR EACH ROW EXECUTE FUNCTION fn_validar_comprobante();

CREATE FUNCTION fn_validar_pago_completo() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_pago UUID; v_estado VARCHAR(15); v_liquidacion UUID;
BEGIN
    v_pago := CASE WHEN TG_TABLE_NAME = 'pagos' THEN COALESCE(NEW.id, OLD.id) ELSE COALESCE(NEW.pago_id, OLD.pago_id) END;
    IF v_pago IS NULL THEN RETURN NULL; END IF;
    SELECT estado, liquidacion_id INTO v_estado, v_liquidacion FROM pagos WHERE id = v_pago;
    IF v_estado = 'APROBADO' THEN
        IF (SELECT count(*) FROM movimientos_caja WHERE pago_id = v_pago) <> 1
           OR (SELECT count(*) FROM comprobantes WHERE pago_id = v_pago) <> 1
           OR NOT EXISTS (SELECT 1 FROM liquidaciones WHERE id = v_liquidacion AND estado = 'PAGADA') THEN
            RAISE EXCEPTION 'Pago aprobado requiere liquidación pagada, movimiento y comprobante';
        END IF;
        UPDATE cargos_abandono c SET estado = 'PAGADO', actualizado_en = now()
        FROM liquidaciones l WHERE l.id = v_liquidacion AND l.cargo_abandono_id = c.id AND c.estado = 'LIQUIDADO';
    END IF;
    RETURN NULL;
END;
$$;
CREATE CONSTRAINT TRIGGER ct_pago_integridad AFTER INSERT OR UPDATE ON pagos DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION fn_validar_pago_completo();
CREATE CONSTRAINT TRIGGER ct_movimiento_integridad AFTER INSERT OR UPDATE ON movimientos_caja DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION fn_validar_pago_completo();
CREATE CONSTRAINT TRIGGER ct_comprobante_integridad AFTER INSERT OR UPDATE ON comprobantes DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION fn_validar_pago_completo();

CREATE FUNCTION fn_proteger_finanzas() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'No se permite borrar o alterar el registro financiero histórico'; END;
$$;
CREATE TRIGGER trg_no_delete_pagos BEFORE DELETE ON pagos FOR EACH ROW EXECUTE FUNCTION fn_proteger_finanzas();
CREATE TRIGGER trg_no_update_comprobantes BEFORE UPDATE OR DELETE ON comprobantes FOR EACH ROW EXECUTE FUNCTION fn_proteger_finanzas();
CREATE TRIGGER trg_no_update_movimientos BEFORE UPDATE OR DELETE ON movimientos_caja FOR EACH ROW EXECUTE FUNCTION fn_proteger_finanzas();
CREATE TRIGGER trg_no_delete_eventos_cargo BEFORE DELETE ON eventos_cargo_abandono FOR EACH ROW EXECUTE FUNCTION fn_proteger_finanzas();

CREATE FUNCTION fn_cerrar_caja_segura() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.estado = 'CERRADA' AND NEW IS DISTINCT FROM OLD THEN RAISE EXCEPTION 'Caja cerrada es terminal'; END IF;
    IF NEW.estado = 'CERRADA' AND EXISTS (
        SELECT 1 FROM pagos p WHERE p.sesion_caja_id = NEW.id AND p.estado = 'PENDIENTE'
    ) THEN RAISE EXCEPTION 'No cerrar caja con pagos pendientes'; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_cerrar_caja_segura BEFORE UPDATE ON sesiones_caja
FOR EACH ROW EXECUTE FUNCTION fn_cerrar_caja_segura();
