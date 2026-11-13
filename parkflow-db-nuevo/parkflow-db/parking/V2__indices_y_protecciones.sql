CREATE UNIQUE INDEX uq_sesion_vehiculo_activa ON sesiones_parqueo(vehiculo_id) WHERE estado = 'ACTIVA';
CREATE UNIQUE INDEX uq_sesion_espacio_activo ON sesiones_parqueo(espacio_id) WHERE estado = 'ACTIVA';
CREATE UNIQUE INDEX uq_solicitud_excepcional_pendiente ON solicitudes_salida_excepcional(sesion_id) WHERE estado = 'PENDIENTE';
CREATE UNIQUE INDEX uq_mantenimiento_activo_espacio ON mantenimientos_espacio(espacio_id) WHERE estado = 'EN_PROCESO';
CREATE INDEX ix_sesiones_fecha_entrada ON sesiones_parqueo(hora_entrada DESC);
CREATE INDEX ix_sesiones_estado ON sesiones_parqueo(estado);
CREATE INDEX ix_solicitudes_estado_fecha ON solicitudes_salida_excepcional(estado, creado_en);
CREATE INDEX ix_eventos_sesion_fecha ON eventos_operativos(sesion_id, creado_en DESC);
CREATE INDEX ix_mantenimientos_espacio_fecha ON mantenimientos_espacio(espacio_id, iniciado_en DESC);
CREATE INDEX ix_espacios_zona_estado ON espacios(zona_id, estado);

CREATE OR REPLACE FUNCTION fn_proteger_sesion_finalizada()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.estado IN ('CERRADA','ANULADA') AND NEW IS DISTINCT FROM OLD THEN
  RAISE EXCEPTION 'Una sesión cerrada o anulada es inmutable';
 END IF;
 RETURN NEW;
END; $$;
CREATE TRIGGER trg_proteger_sesion_finalizada
BEFORE UPDATE ON sesiones_parqueo
FOR EACH ROW EXECUTE FUNCTION fn_proteger_sesion_finalizada();

CREATE OR REPLACE FUNCTION fn_bloquear_borrado_historico()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'No se permite eliminar registros históricos de ParkFlow';
END; $$;
CREATE TRIGGER trg_no_delete_eventos BEFORE DELETE ON eventos_operativos FOR EACH ROW EXECUTE FUNCTION fn_bloquear_borrado_historico();
CREATE TRIGGER trg_no_delete_traslados BEFORE DELETE ON traslados_espacio FOR EACH ROW EXECUTE FUNCTION fn_bloquear_borrado_historico();
CREATE TRIGGER trg_no_delete_anulaciones BEFORE DELETE ON anulaciones_sesion FOR EACH ROW EXECUTE FUNCTION fn_bloquear_borrado_historico();
CREATE TRIGGER trg_no_delete_salidas_excepcionales BEFORE DELETE ON salidas_excepcionales FOR EACH ROW EXECUTE FUNCTION fn_bloquear_borrado_historico();
