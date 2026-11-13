CREATE INDEX ix_usuario_roles_rol ON usuario_roles(rol_id);
CREATE INDEX ix_autorizaciones_estado_expira ON autorizaciones_excepcionales(estado, expira_en);
CREATE INDEX ix_auditoria_usuario_fecha ON auditoria_seguridad(usuario_id, creado_en DESC);
CREATE INDEX ix_auditoria_evento_fecha ON auditoria_seguridad(evento, creado_en DESC);

CREATE OR REPLACE FUNCTION fn_bloquear_borrado_auditoria()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'La auditoría de seguridad es inmutable'; END; $$;
CREATE TRIGGER trg_auditoria_seguridad_no_delete BEFORE DELETE ON auditoria_seguridad FOR EACH ROW EXECUTE FUNCTION fn_bloquear_borrado_auditoria();

CREATE OR REPLACE FUNCTION fn_bloquear_borrado_autorizacion()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Las autorizaciones históricas no se eliminan'; END; $$;
CREATE TRIGGER trg_autorizacion_no_delete BEFORE DELETE ON autorizaciones_excepcionales FOR EACH ROW EXECUTE FUNCTION fn_bloquear_borrado_autorizacion();
