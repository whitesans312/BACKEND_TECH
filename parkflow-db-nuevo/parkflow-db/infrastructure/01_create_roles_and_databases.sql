-- Ejecutar como administrador PostgreSQL (por ejemplo, postgres).
-- Las contraseñas se configuran fuera del repositorio; no escribirlas aquí.
CREATE ROLE parking_migrator LOGIN;
CREATE ROLE parking_app LOGIN;
CREATE ROLE identity_migrator LOGIN;
CREATE ROLE identity_app LOGIN;
CREATE ROLE billing_migrator LOGIN;
CREATE ROLE billing_app LOGIN;

CREATE DATABASE db_parking OWNER parking_migrator ENCODING 'UTF8';
CREATE DATABASE db_identity OWNER identity_migrator ENCODING 'UTF8';
CREATE DATABASE db_billing OWNER billing_migrator ENCODING 'UTF8';

-- Después de crear las bases, ejecutar los GRANT de cada archivo V3
-- conectándose individualmente a su respectiva base.
