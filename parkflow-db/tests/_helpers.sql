-- Ayudantes de prueba (se crean en el esquema temporal de la sesión)
CREATE FUNCTION pg_temp.expect(p_sql text, p_state text, p_name text, p_constraint text DEFAULT NULL)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE v_c text;
BEGIN
  BEGIN
    EXECUTE p_sql;
  EXCEPTION WHEN OTHERS THEN
    GET STACKED DIAGNOSTICS v_c = CONSTRAINT_NAME;
    IF SQLSTATE = p_state AND (p_constraint IS NULL OR v_c = p_constraint) THEN
      RAISE NOTICE 'OK     %', p_name; RETURN;
    END IF;
    RAISE EXCEPTION USING ERRCODE = 'XX999',
      MESSAGE = format('FALLO  %s: error inesperado %s (restriccion=%s) %s', p_name, SQLSTATE, v_c, SQLERRM);
  END;
  RAISE EXCEPTION USING ERRCODE = 'XX999', MESSAGE = format('FALLO  %s: debia fallar y no fallo', p_name);
END $$;

CREATE FUNCTION pg_temp.expect_ok(p_sql text, p_name text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  BEGIN
    EXECUTE p_sql;
  EXCEPTION WHEN OTHERS THEN
    RAISE EXCEPTION USING ERRCODE = 'XX999',
      MESSAGE = format('FALLO  %s: debia funcionar y fallo con %s %s', p_name, SQLSTATE, SQLERRM);
  END;
  RAISE NOTICE 'OK     %', p_name;
END $$;

CREATE FUNCTION pg_temp.assert_eq(p_actual text, p_expected text, p_name text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  IF p_actual IS DISTINCT FROM p_expected THEN
    RAISE EXCEPTION USING ERRCODE = 'XX999',
      MESSAGE = format('FALLO  %s: esperado=%s obtenido=%s', p_name, p_expected, p_actual);
  END IF;
  RAISE NOTICE 'OK     %', p_name;
END $$;
