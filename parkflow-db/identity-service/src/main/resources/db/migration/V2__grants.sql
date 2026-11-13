-- Sin DELETE en tablas de negocio (RN-I3). EXCEPCIÓN A CONFIRMAR: usuario_roles permite DELETE
-- porque, de lo contrario, no se podría quitar un rol a un usuario (ver informe de pruebas).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'identity_app') THEN
        GRANT USAGE ON SCHEMA public TO identity_app;
        GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO identity_app;
        GRANT DELETE ON usuario_roles TO identity_app;
    END IF;
END $$;
