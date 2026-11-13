-- Solo para una instancia/local vacía o administrada explícitamente.
-- No contiene DROP y no borra datos. Contraseñas exclusivamente de desarrollo.
CREATE ROLE parking_migrator LOGIN PASSWORD 'parking_migrator_dev_only';
CREATE ROLE parking_app LOGIN PASSWORD 'parking_app_dev_only';
CREATE ROLE identity_migrator LOGIN PASSWORD 'identity_migrator_dev_only';
CREATE ROLE identity_app LOGIN PASSWORD 'identity_app_dev_only';
CREATE ROLE billing_migrator LOGIN PASSWORD 'billing_migrator_dev_only';
CREATE ROLE billing_app LOGIN PASSWORD 'billing_app_dev_only';

CREATE DATABASE db_parking OWNER parking_migrator ENCODING 'UTF8';
CREATE DATABASE db_identity OWNER identity_migrator ENCODING 'UTF8';
CREATE DATABASE db_billing OWNER billing_migrator ENCODING 'UTF8';

REVOKE CONNECT ON DATABASE db_parking FROM PUBLIC;
REVOKE CONNECT ON DATABASE db_identity FROM PUBLIC;
REVOKE CONNECT ON DATABASE db_billing FROM PUBLIC;
GRANT CONNECT ON DATABASE db_parking TO parking_migrator, parking_app;
GRANT CONNECT ON DATABASE db_identity TO identity_migrator, identity_app;
GRANT CONNECT ON DATABASE db_billing TO billing_migrator, billing_app;
