-- Ejecutar como superusuario local (postgres). Solo para desarrollo local.
-- Cambie las contraseñas; en Azure se usan secretos, nunca contraseñas en el repositorio.
DO $$
DECLARE r text;
BEGIN
  FOREACH r IN ARRAY ARRAY['parking_app','billing_app','identity_app'] LOOP
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = r) THEN
      EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', r, 'cambiar_' || r);
    END IF;
  END LOOP;
END $$;
