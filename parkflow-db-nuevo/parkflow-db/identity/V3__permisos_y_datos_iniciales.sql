-- Ejecutar conectado a db_identity como identity_migrator.
GRANT CONNECT ON DATABASE db_identity TO identity_app;
GRANT USAGE ON SCHEMA public TO identity_app;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO identity_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO identity_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO identity_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO identity_app;
REVOKE DELETE ON ALL TABLES IN SCHEMA public FROM identity_app;

INSERT INTO roles(nombre, descripcion) VALUES
 ('ADMINISTRADOR','Administración general del parqueadero'),
 ('OPERADOR','Operación diaria de ingreso, salida y caja');
