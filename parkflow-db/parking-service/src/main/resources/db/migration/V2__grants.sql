-- Permisos del usuario de aplicación: sin DELETE (RN-P11). Se omite si el rol no existe.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'parking_app') THEN
        GRANT USAGE ON SCHEMA public TO parking_app;
        GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO parking_app;
        GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO parking_app;
    END IF;
END $$;
