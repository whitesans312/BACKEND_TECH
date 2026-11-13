REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO identity_app;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO identity_app;
REVOKE DELETE ON ALL TABLES IN SCHEMA public FROM identity_app;
ALTER DEFAULT PRIVILEGES FOR ROLE identity_migrator IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO identity_app;
ALTER DEFAULT PRIVILEGES FOR ROLE identity_migrator IN SCHEMA public REVOKE DELETE ON TABLES FROM identity_app;

INSERT INTO roles(nombre, descripcion) VALUES
    ('ADMINISTRADOR','Administración general del parqueadero'),
    ('OPERADOR','Operación diaria de ingreso, salida y caja');

INSERT INTO auditoria_seguridad(evento, detalle) VALUES
    ('CONFIGURACION_INICIAL', '{"roles":"ADMINISTRADOR,OPERADOR"}'::jsonb);
