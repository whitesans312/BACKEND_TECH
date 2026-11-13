CREATE OR REPLACE FUNCTION fn_validar_pago_completo() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_pago UUID; v_estado VARCHAR(15); v_liquidacion UUID;
BEGIN
    IF TG_TABLE_NAME = 'pagos' THEN
        v_pago := NEW.id;
    ELSE
        v_pago := NEW.pago_id;
    END IF;
    IF v_pago IS NULL THEN RETURN NULL; END IF;
    SELECT estado, liquidacion_id INTO v_estado, v_liquidacion FROM pagos WHERE id = v_pago;
    IF v_estado = 'APROBADO' THEN
        IF (SELECT count(*) FROM movimientos_caja WHERE pago_id = v_pago) <> 1
           OR (SELECT count(*) FROM comprobantes WHERE pago_id = v_pago) <> 1
           OR NOT EXISTS (SELECT 1 FROM liquidaciones WHERE id = v_liquidacion AND estado = 'PAGADA') THEN
            RAISE EXCEPTION 'Pago aprobado requiere liquidación pagada, movimiento y comprobante';
        END IF;
        UPDATE cargos_abandono c SET estado = 'PAGADO', actualizado_en = now()
        FROM liquidaciones l WHERE l.id = v_liquidacion AND l.cargo_abandono_id = c.id AND c.estado = 'LIQUIDADO';
    END IF;
    RETURN NULL;
END;
$$;
