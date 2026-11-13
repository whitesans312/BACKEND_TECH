CREATE UNIQUE INDEX uq_sesion_placa_activa ON sesiones_parqueo(placa_actual) WHERE estado = 'ACTIVA';
CREATE UNIQUE INDEX uq_sesion_espacio_activo ON sesiones_parqueo(espacio_id) WHERE estado = 'ACTIVA';
CREATE UNIQUE INDEX uq_solicitud_excepcional_pendiente ON solicitudes_salida_excepcional(sesion_id) WHERE estado = 'PENDIENTE';
CREATE UNIQUE INDEX uq_mantenimiento_activo_espacio ON mantenimientos_espacio(espacio_id) WHERE estado = 'EN_PROCESO';
CREATE INDEX ix_sesiones_estado_entrada ON sesiones_parqueo(estado, hora_entrada DESC);
CREATE INDEX ix_sesiones_placa_entrada ON sesiones_parqueo(placa_actual, hora_entrada DESC);
CREATE INDEX ix_espacios_zona_estado ON espacios(zona_id, estado, tipo_vehiculo);
CREATE INDEX ix_eventos_sesion_fecha ON eventos_operativos(sesion_id, creado_en DESC);
CREATE INDEX ix_mantenimientos_espacio_fecha ON mantenimientos_espacio(espacio_id, iniciado_en DESC);
CREATE INDEX ix_abandono_estado_fecha ON casos_abandono(estado, reportado_en DESC);
CREATE INDEX ix_contactos_caso_fecha ON contactos_autoridad(caso_abandono_id, contactado_en DESC);
CREATE INDEX ix_evidencias_caso_fecha ON evidencias_abandono(caso_abandono_id, creado_en DESC);
CREATE INDEX ix_traslados_sesion_fecha ON traslados_espacio(sesion_id, trasladado_en DESC);

CREATE FUNCTION fn_validar_cierre_sesion() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.estado = 'CERRADA' AND NEW.tipo_cierre = 'ABANDONO' THEN
        IF NOT EXISTS (
            SELECT 1 FROM casos_abandono c
            JOIN retiros_vehiculo r ON r.caso_abandono_id = c.id
            WHERE c.sesion_id = NEW.id AND c.estado = 'CERRADO'
              AND c.operacion_cierre_id = NEW.operacion_cierre_id
        ) THEN
            RAISE EXCEPTION 'Cierre ABANDONO requiere caso cerrado y retiro físico';
        END IF;
    ELSIF NEW.estado = 'CERRADA' AND NEW.tipo_cierre = 'EXCEPCIONAL' THEN
        IF NOT EXISTS (SELECT 1 FROM salidas_excepcionales s WHERE s.sesion_id = NEW.id) THEN
            RAISE EXCEPTION 'Cierre EXCEPCIONAL requiere su registro';
        END IF;
    ELSIF NEW.estado = 'ANULADA' THEN
        IF NOT EXISTS (SELECT 1 FROM anulaciones_sesion a WHERE a.sesion_id = NEW.id) THEN
            RAISE EXCEPTION 'Sesión ANULADA requiere su registro';
        END IF;
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER ct_validar_cierre_sesion
AFTER INSERT OR UPDATE ON sesiones_parqueo
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION fn_validar_cierre_sesion();

CREATE FUNCTION fn_transicion_sesion() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.estado IN ('CERRADA','ANULADA') AND NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'Sesión cerrada o anulada es terminal';
    END IF;
    IF OLD.estado = 'ACTIVA' AND NEW.estado NOT IN ('ACTIVA','CERRADA','ANULADA') THEN
        RAISE EXCEPTION 'Transición de sesión no permitida';
    END IF;
    IF OLD.estado = 'ACTIVA' AND NEW.estado = 'CERRADA' THEN
        IF NEW.tipo_cierre IN ('PAGADA','GRATUITA') AND NEW.liquidacion_id IS NULL THEN
            RAISE EXCEPTION 'Cierre pagado/gratuito requiere liquidación Billing';
        END IF;
        IF NEW.tipo_cierre = 'EXCEPCIONAL' AND NOT EXISTS (
            SELECT 1 FROM salidas_excepcionales s WHERE s.sesion_id = NEW.id
        ) THEN RAISE EXCEPTION 'Salida excepcional requiere evidencia y autorización'; END IF;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_transicion_sesion BEFORE UPDATE ON sesiones_parqueo
FOR EACH ROW EXECUTE FUNCTION fn_transicion_sesion();

CREATE FUNCTION fn_transicion_caso_abandono() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_siguiente VARCHAR(30);
BEGIN
    IF OLD.estado = 'CERRADO' AND NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'El caso de abandono cerrado es inmutable';
    END IF;
    v_siguiente := CASE OLD.estado
        WHEN 'ABANDONO_REPORTADO' THEN 'EN_GESTION'
        WHEN 'EN_GESTION' THEN 'AUTORIDAD_CONTACTADA'
        WHEN 'AUTORIDAD_CONTACTADA' THEN 'RETIRO_COORDINADO'
        WHEN 'RETIRO_COORDINADO' THEN 'VEHICULO_RETIRADO'
        WHEN 'VEHICULO_RETIRADO' THEN 'CERRADO'
        ELSE NULL
    END;
    IF NEW.estado <> OLD.estado AND NEW.estado IS DISTINCT FROM v_siguiente THEN
        RAISE EXCEPTION 'Transición de abandono no permitida: % -> %', OLD.estado, NEW.estado;
    END IF;
    IF NEW.estado = 'CERRADO' AND NOT EXISTS (
        SELECT 1 FROM retiros_vehiculo r WHERE r.caso_abandono_id = NEW.id
    ) THEN
        RAISE EXCEPTION 'El caso solo se cierra después del retiro físico';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_transicion_caso_abandono BEFORE UPDATE ON casos_abandono
FOR EACH ROW EXECUTE FUNCTION fn_transicion_caso_abandono();

CREATE FUNCTION fn_prohibir_borrado_historico() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'No se permite borrar historial de ParkFlow'; END;
$$;
CREATE TRIGGER trg_no_delete_eventos BEFORE DELETE ON eventos_operativos FOR EACH ROW EXECUTE FUNCTION fn_prohibir_borrado_historico();
CREATE TRIGGER trg_no_delete_traslados BEFORE DELETE ON traslados_espacio FOR EACH ROW EXECUTE FUNCTION fn_prohibir_borrado_historico();
CREATE TRIGGER trg_no_delete_abandono BEFORE DELETE ON casos_abandono FOR EACH ROW EXECUTE FUNCTION fn_prohibir_borrado_historico();
CREATE TRIGGER trg_no_delete_evidencias BEFORE DELETE ON evidencias_abandono FOR EACH ROW EXECUTE FUNCTION fn_prohibir_borrado_historico();
CREATE TRIGGER trg_no_delete_retiros BEFORE DELETE ON retiros_vehiculo FOR EACH ROW EXECUTE FUNCTION fn_prohibir_borrado_historico();
