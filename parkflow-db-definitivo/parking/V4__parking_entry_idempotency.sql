ALTER TABLE sesiones_parqueo ADD COLUMN clave_idempotencia UUID;
CREATE UNIQUE INDEX uq_sesiones_clave_idempotencia ON sesiones_parqueo(clave_idempotencia) WHERE clave_idempotencia IS NOT NULL;
