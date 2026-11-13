-- ParkFlow · db_billing · V1 · esquema inicial
CREATE SEQUENCE seq_comprobante AS bigint START 1 INCREMENT 1;

CREATE TABLE tarifas (
    id              uuid          NOT NULL DEFAULT gen_random_uuid(),
    tipo_vehiculo   varchar(20)   NOT NULL,
    unidad_minutos  smallint      NOT NULL,
    valor_unidad    numeric(12,2) NOT NULL,
    minutos_gracia  smallint      NOT NULL DEFAULT 0,
    moneda          char(3)       NOT NULL DEFAULT 'COP',
    activa          boolean       NOT NULL DEFAULT true,
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_tarifas PRIMARY KEY (id),
    CONSTRAINT ck_tarifas_tipo    CHECK (tipo_vehiculo IN ('CARRO', 'MOTO')),
    CONSTRAINT ck_tarifas_unidad  CHECK (unidad_minutos > 0),
    CONSTRAINT ck_tarifas_valor   CHECK (valor_unidad >= 0),
    CONSTRAINT ck_tarifas_gracia  CHECK (minutos_gracia >= 0),
    CONSTRAINT ck_tarifas_moneda  CHECK (moneda ~ '^[A-Z]{3}$')
);
-- RN-B1: una sola tarifa vigente por tipo de vehículo
CREATE UNIQUE INDEX uq_tarifa_vigente ON tarifas (tipo_vehiculo) WHERE activa;

CREATE TABLE pagos (
    id                      uuid          NOT NULL DEFAULT gen_random_uuid(),
    sesion_id               uuid          NOT NULL,   -- externo: Parking (sin FK)
    numero_ticket           varchar(30)   NOT NULL,
    idempotency_key         uuid          NOT NULL,
    request_hash            char(64)      NOT NULL,
    estado                  varchar(20)   NOT NULL DEFAULT 'PENDIENTE',
    metodo                  varchar(20),
    tarifa_id               uuid          NOT NULL,
    tipo_vehiculo           varchar(20)   NOT NULL,
    placa                   varchar(10)   NOT NULL,
    fecha_entrada           timestamptz   NOT NULL,
    fecha_corte             timestamptz   NOT NULL,
    minutos_permanencia     integer       NOT NULL,
    unidad_minutos          smallint      NOT NULL,
    valor_unidad            numeric(12,2) NOT NULL,
    minutos_gracia          smallint      NOT NULL,
    moneda                  char(3)       NOT NULL,
    unidades_cobradas       integer       NOT NULL,
    valor_total             numeric(12,2) NOT NULL,
    operador_id             uuid          NOT NULL,   -- externo: Identity (sin FK)
    operador_resolucion_id  uuid,                     -- externo: Identity (sin FK)
    fecha_resolucion        timestamptz,
    motivo_rechazo          varchar(200),
    created_at              timestamptz   NOT NULL DEFAULT now(),
    updated_at              timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_pagos PRIMARY KEY (id),
    CONSTRAINT uq_pagos_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_pagos_tarifa FOREIGN KEY (tarifa_id) REFERENCES tarifas (id) ON DELETE RESTRICT,
    CONSTRAINT ck_pagos_ticket  CHECK (numero_ticket ~ '^PF-[0-9]{4}-[0-9]{6,}$'),
    CONSTRAINT ck_pagos_hash    CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_pagos_estado  CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
    CONSTRAINT ck_pagos_metodo  CHECK (metodo IS NULL OR metodo IN ('EFECTIVO', 'TARJETA', 'SIN_COSTO')),
    CONSTRAINT ck_pagos_tipo    CHECK (tipo_vehiculo IN ('CARRO', 'MOTO')),
    CONSTRAINT ck_pagos_placa   CHECK (placa ~ '^[A-Z0-9]{4,10}$'),
    CONSTRAINT ck_pagos_corte   CHECK (fecha_corte >= fecha_entrada),
    CONSTRAINT ck_pagos_minutos CHECK (minutos_permanencia >= 0),
    CONSTRAINT ck_pagos_unidad  CHECK (unidad_minutos > 0 AND valor_unidad >= 0 AND minutos_gracia >= 0),
    CONSTRAINT ck_pagos_moneda  CHECK (moneda ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_pagos_unidades CHECK (unidades_cobradas >= 0),
    CONSTRAINT ck_pagos_valor   CHECK (valor_total >= 0 AND valor_total = unidades_cobradas * valor_unidad),
    -- RN-B2: la fórmula calcula Java; PostgreSQL comprueba que lo guardado concuerda
    CONSTRAINT ck_pagos_formula_minutos CHECK (
        minutos_permanencia = floor(extract(epoch FROM (fecha_corte - fecha_entrada)) / 60)),
    CONSTRAINT ck_pagos_formula_unidades CHECK (
        unidades_cobradas = ceil(greatest(0, minutos_permanencia - minutos_gracia)::numeric / unidad_minutos)),
    CONSTRAINT ck_pagos_coherencia CHECK (
        (estado = 'PENDIENTE'
            AND metodo IS NULL AND fecha_resolucion IS NULL
            AND operador_resolucion_id IS NULL AND motivo_rechazo IS NULL)
     OR (estado = 'APROBADO'
            AND metodo IS NOT NULL AND fecha_resolucion IS NOT NULL
            AND operador_resolucion_id IS NOT NULL AND motivo_rechazo IS NULL
            AND ((metodo = 'SIN_COSTO') = (valor_total = 0)))
     OR (estado = 'RECHAZADO'
            AND fecha_resolucion IS NOT NULL AND operador_resolucion_id IS NOT NULL
            AND motivo_rechazo IS NOT NULL AND btrim(motivo_rechazo) <> ''
            AND metodo IS NULL)
    )
);
-- RN-B5: un solo pago PENDIENTE o APROBADO por sesión
CREATE UNIQUE INDEX uq_pago_sesion_vigente ON pagos (sesion_id) WHERE estado IN ('PENDIENTE', 'APROBADO');
CREATE INDEX idx_pagos_sesion ON pagos (sesion_id);
CREATE INDEX idx_pagos_resolucion ON pagos (fecha_resolucion);

CREATE TABLE comprobantes (
    id             uuid        NOT NULL DEFAULT gen_random_uuid(),
    pago_id        uuid        NOT NULL,
    numero         varchar(30) NOT NULL,
    fecha_emision  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_comprobantes PRIMARY KEY (id),
    CONSTRAINT uq_comprobantes_pago UNIQUE (pago_id),
    CONSTRAINT uq_comprobantes_numero UNIQUE (numero),
    CONSTRAINT fk_comprobantes_pago FOREIGN KEY (pago_id) REFERENCES pagos (id) ON DELETE RESTRICT,
    CONSTRAINT ck_comprobantes_numero CHECK (numero ~ '^PF-CMP-[0-9]{4}-[0-9]{6,}$')
);

-- ============ Triggers (solo validan o impiden) ============

-- Tarifas históricas inmutables: solo activa true -> false.
CREATE FUNCTION fn_tarifa_inmutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (to_jsonb(NEW) - 'activa' - 'updated_at') <> (to_jsonb(OLD) - 'activa' - 'updated_at') THEN
        RAISE EXCEPTION 'Los valores de una tarifa son inmutables: desactive y cree otra'
            USING ERRCODE = 'check_violation';
    END IF;
    IF OLD.activa = false AND NEW.activa = true THEN
        RAISE EXCEPTION 'Una tarifa desactivada no se reactiva: cree una nueva'
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_tarifa_inmutable BEFORE UPDATE ON tarifas
    FOR EACH ROW EXECUTE FUNCTION fn_tarifa_inmutable();

-- Pagos: solo PENDIENTE -> APROBADO|RECHAZADO; snapshot, clave y hash inmutables; resuelto = definitivo.
CREATE FUNCTION fn_pago_inmutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.estado <> 'PENDIENTE' THEN
        RAISE EXCEPTION 'El pago % esta % y no puede modificarse', OLD.id, OLD.estado
            USING ERRCODE = 'check_violation';
    END IF;
    IF (to_jsonb(NEW) - 'estado' - 'metodo' - 'operador_resolucion_id'
                      - 'fecha_resolucion' - 'motivo_rechazo' - 'updated_at')
       <> (to_jsonb(OLD) - 'estado' - 'metodo' - 'operador_resolucion_id'
                      - 'fecha_resolucion' - 'motivo_rechazo' - 'updated_at') THEN
        RAISE EXCEPTION 'Snapshot, idempotency_key y request_hash del pago son inmutables'
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_pago_inmutable BEFORE UPDATE ON pagos
    FOR EACH ROW EXECUTE FUNCTION fn_pago_inmutable();

-- Comprobante solo para pagos APROBADOS; el comprobante es inmutable.
CREATE FUNCTION fn_comprobante_validar() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_estado varchar(20);
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'Los comprobantes son inmutables' USING ERRCODE = 'check_violation';
    END IF;
    SELECT estado INTO v_estado FROM pagos WHERE id = NEW.pago_id;
    IF v_estado IS DISTINCT FROM 'APROBADO' THEN
        RAISE EXCEPTION 'Solo un pago APROBADO puede tener comprobante (estado actual: %)', v_estado
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_comprobante_validar BEFORE INSERT OR UPDATE ON comprobantes
    FOR EACH ROW EXECUTE FUNCTION fn_comprobante_validar();

-- R2: comprobación diferida. Al confirmar, todo pago APROBADO debe tener comprobante.
CREATE FUNCTION fn_pago_aprobado_con_comprobante() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.estado = 'APROBADO'
       AND NOT EXISTS (SELECT 1 FROM comprobantes WHERE pago_id = NEW.id) THEN
        RAISE EXCEPTION 'El pago aprobado % no tiene comprobante', NEW.id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ctrg_pago_aprobado_con_comprobante
    AFTER INSERT OR UPDATE ON pagos
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION fn_pago_aprobado_con_comprobante();
-- Permisos del usuario de aplicación: sin DELETE (RN-B11). Se omite si el rol no existe.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'billing_app') THEN
        GRANT USAGE ON SCHEMA public TO billing_app;
        GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO billing_app;
        GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO billing_app;
    END IF;
END $$;
