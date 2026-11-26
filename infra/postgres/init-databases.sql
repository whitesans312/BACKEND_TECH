-- Inicialización idempotente de roles y bases de datos aisladas para ParkFlow.
-- Mantiene los privilegios mínimos por microservicio requeridos por las migraciones Flyway (V3).

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'identity_migrator') THEN
        CREATE ROLE identity_migrator LOGIN PASSWORD 'identity_migrator_dev_only';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'identity_app') THEN
        CREATE ROLE identity_app LOGIN PASSWORD 'identity_app_dev_only';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'parking_migrator') THEN
        CREATE ROLE parking_migrator LOGIN PASSWORD 'parking_migrator_dev_only';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'parking_app') THEN
        CREATE ROLE parking_app LOGIN PASSWORD 'parking_app_dev_only';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'billing_migrator') THEN
        CREATE ROLE billing_migrator LOGIN PASSWORD 'billing_migrator_dev_only';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'billing_app') THEN
        CREATE ROLE billing_app LOGIN PASSWORD 'billing_app_dev_only';
    END IF;
END
$$;

SELECT 'CREATE DATABASE db_identity OWNER identity_migrator ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'db_identity')\gexec

SELECT 'CREATE DATABASE db_parking OWNER parking_migrator ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'db_parking')\gexec

SELECT 'CREATE DATABASE db_billing OWNER billing_migrator ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'db_billing')\gexec

REVOKE CONNECT ON DATABASE db_identity FROM PUBLIC;
REVOKE CONNECT ON DATABASE db_parking FROM PUBLIC;
REVOKE CONNECT ON DATABASE db_billing FROM PUBLIC;

GRANT CONNECT ON DATABASE db_identity TO identity_migrator, identity_app;
GRANT CONNECT ON DATABASE db_parking TO parking_migrator, parking_app;
GRANT CONNECT ON DATABASE db_billing TO billing_migrator, billing_app;
