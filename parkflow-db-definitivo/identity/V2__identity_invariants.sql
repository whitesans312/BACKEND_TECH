CREATE INDEX ix_usuario_roles_rol ON usuario_roles(rol_id);
CREATE INDEX ix_autorizaciones_ex_estado_expira ON autorizaciones_excepcionales(estado, expira_en);
CREATE INDEX ix_autorizaciones_cargo_caso ON autorizaciones_cargo_abandono(caso_abandono_id, autorizada_en DESC);
CREATE INDEX ix_auditoria_usuario_fecha ON auditoria_seguridad(usuario_id, creado_en DESC);
CREATE INDEX ix_auditoria_evento_fecha ON auditoria_seguridad(evento, creado_en DESC);

CREATE FUNCTION fn_consumir_autorizacion() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.estado IN ('CONSUMIDA','REVOCADA','EXPIRADA') AND NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'Autorización excepcional terminal es inmutable';
    END IF;
    IF OLD.estado = 'EMITIDA' AND NEW.estado = 'CONSUMIDA' THEN
        IF NEW.consumida_en IS NULL OR NEW.operacion_id <> OLD.operacion_id OR NEW.sesion_parking_id <> OLD.sesion_parking_id THEN
            RAISE EXCEPTION 'El consumo debe conservar operación y sesión autorizadas';
        END IF;
    ELSIF OLD.estado = 'EMITIDA' AND NEW.estado IN ('REVOCADA','EXPIRADA') THEN
        IF NEW.operacion_id <> OLD.operacion_id OR NEW.sesion_parking_id <> OLD.sesion_parking_id OR NEW.consumida_en IS NOT NULL THEN
            RAISE EXCEPTION 'Revocación/vencimiento no puede alterar identidad de la operación';
        END IF;
    ELSIF NEW.estado <> OLD.estado THEN
        RAISE EXCEPTION 'Transición de autorización no permitida';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_consumir_autorizacion BEFORE UPDATE ON autorizaciones_excepcionales
FOR EACH ROW EXECUTE FUNCTION fn_consumir_autorizacion();

CREATE FUNCTION fn_proteger_auditoria() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Auditoría de seguridad es append-only'; END;
$$;
CREATE TRIGGER trg_auditoria_no_update_delete BEFORE UPDATE OR DELETE ON auditoria_seguridad
FOR EACH ROW EXECUTE FUNCTION fn_proteger_auditoria();
CREATE TRIGGER trg_autorizacion_cargo_no_delete BEFORE DELETE ON autorizaciones_cargo_abandono
FOR EACH ROW EXECUTE FUNCTION fn_proteger_auditoria();
