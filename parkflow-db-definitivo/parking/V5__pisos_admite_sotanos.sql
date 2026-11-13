ALTER TABLE pisos DROP CONSTRAINT IF EXISTS pisos_numero_check;
ALTER TABLE pisos ADD CONSTRAINT ck_pisos_numero CHECK (numero >= -5);
