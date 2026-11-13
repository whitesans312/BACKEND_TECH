-- ParkFlow · db_parking · V1 · esquema inicial
-- Sin extensiones, sin superusuario. gen_random_uuid() es nativo (PostgreSQL 13+).

CREATE SEQUENCE seq_ticket AS bigint START 1 INCREMENT 1;

CREATE TABLE pisos (
    id          uuid        NOT NULL DEFAULT gen_random_uuid(),
    codigo      varchar(10) NOT NULL,
    nombre      varchar(60) NOT NULL,
    orden       smallint    NOT NULL,
    habilitado  boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_pisos PRIMARY KEY (id),
    CONSTRAINT uq_pisos_codigo UNIQUE (codigo),
    CONSTRAINT uq_pisos_orden UNIQUE (orden),
    CONSTRAINT ck_pisos_codigo CHECK (codigo <> '' AND codigo = btrim(codigo)),
    CONSTRAINT ck_pisos_nombre CHECK (btrim(nombre) <> ''),
    CONSTRAINT ck_pisos_orden  CHECK (orden > 0)
);

CREATE TABLE espacios (
    id          uuid        NOT NULL DEFAULT gen_random_uuid(),
    piso_id     uuid        NOT NULL,
    codigo      varchar(10) NOT NULL,
    tipo        varchar(20) NOT NULL,
    habilitado  boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_espacios PRIMARY KEY (id),
    CONSTRAINT fk_espacios_piso FOREIGN KEY (piso_id) REFERENCES pisos (id) ON DELETE RESTRICT,
    CONSTRAINT uq_espacios_codigo UNIQUE (codigo),
    CONSTRAINT ck_espacios_codigo CHECK (codigo <> '' AND codigo = btrim(codigo)),
    CONSTRAINT ck_espacios_tipo   CHECK (tipo IN ('CARRO', 'MOTO'))
);
CREATE INDEX idx_espacios_piso ON espacios (piso_id);
CREATE INDEX idx_espacios_asignacion ON espacios (piso_id, tipo, codigo) WHERE habilitado;

CREATE TABLE vehiculos (
    id          uuid        NOT NULL DEFAULT gen_random_uuid(),
    placa       varchar(10) NOT NULL,
    tipo        varchar(20) NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_vehiculos PRIMARY KEY (id),
    CONSTRAINT uq_vehiculos_placa UNIQUE (placa),
    CONSTRAINT ck_vehiculos_placa CHECK (placa ~ '^[A-Z0-9]{4,10}$'),
    CONSTRAINT ck_vehiculos_tipo  CHECK (tipo IN ('CARRO', 'MOTO'))
);

CREATE TABLE sesiones_parqueo (
    id                   uuid         NOT NULL DEFAULT gen_random_uuid(),
    numero_ticket        varchar(30)  NOT NULL,
    vehiculo_id          uuid         NOT NULL,
    espacio_id           uuid         NOT NULL,
    estado               varchar(20)  NOT NULL DEFAULT 'ACTIVA',
    fecha_entrada        timestamptz  NOT NULL DEFAULT now(),
    fecha_salida         timestamptz,
    pago_id              uuid,                 -- externo: Billing (sin FK)
    operador_entrada_id  uuid         NOT NULL, -- externo: Identity (sin FK)
    operador_cierre_id   uuid,                 -- externo: Identity (sin FK)
    motivo_anulacion     varchar(200),
    created_at           timestamptz  NOT NULL DEFAULT now(),
    updated_at           timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_sesiones PRIMARY KEY (id),
    CONSTRAINT uq_sesiones_ticket UNIQUE (numero_ticket),
    CONSTRAINT fk_sesiones_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculos (id) ON DELETE RESTRICT,
    CONSTRAINT fk_sesiones_espacio  FOREIGN KEY (espacio_id)  REFERENCES espacios (id)  ON DELETE RESTRICT,
    CONSTRAINT ck_sesiones_ticket CHECK (numero_ticket ~ '^PF-[0-9]{4}-[0-9]{6,}$'),
    CONSTRAINT ck_sesiones_estado CHECK (estado IN ('ACTIVA', 'CERRADA', 'ANULADA')),
    CONSTRAINT ck_sesiones_fechas CHECK (fecha_salida IS NULL OR fecha_salida >= fecha_entrada),
    CONSTRAINT ck_sesiones_coherencia CHECK (
        (estado = 'ACTIVA'
            AND fecha_salida IS NULL AND pago_id IS NULL
            AND operador_cierre_id IS NULL AND motivo_anulacion IS NULL)
     OR (estado = 'CERRADA'
            AND fecha_salida IS NOT NULL AND pago_id IS NOT NULL
            AND operador_cierre_id IS NOT NULL AND motivo_anulacion IS NULL)
     OR (estado = 'ANULADA'
            AND motivo_anulacion IS NOT NULL AND btrim(motivo_anulacion) <> ''
            AND operador_cierre_id IS NOT NULL
            AND fecha_salida IS NULL AND pago_id IS NULL)
    )
);
-- RN-P1 y RN-P2: una sola sesión ACTIVA por vehículo y por espacio
CREATE UNIQUE INDEX uq_sesion_activa_vehiculo ON sesiones_parqueo (vehiculo_id) WHERE estado = 'ACTIVA';
CREATE UNIQUE INDEX uq_sesion_activa_espacio  ON sesiones_parqueo (espacio_id)  WHERE estado = 'ACTIVA';
CREATE UNIQUE INDEX uq_sesion_pago ON sesiones_parqueo (pago_id) WHERE pago_id IS NOT NULL;
CREATE INDEX idx_sesion_vehiculo_hist ON sesiones_parqueo (vehiculo_id, fecha_entrada DESC);
CREATE INDEX idx_sesion_entrada ON sesiones_parqueo (fecha_entrada DESC);

-- ============ Triggers (solo validan o impiden) ============

-- Al insertar: tipo de vehículo = tipo de espacio; si ACTIVA, espacio y piso habilitados.
CREATE FUNCTION fn_sesion_validar_insert() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_tipo_veh  varchar(20);
    v_tipo_esp  varchar(20);
    v_esp_hab   boolean;
    v_piso_hab  boolean;
BEGIN
    SELECT tipo INTO v_tipo_veh FROM vehiculos WHERE id = NEW.vehiculo_id;
    SELECT e.tipo, e.habilitado, p.habilitado
      INTO v_tipo_esp, v_esp_hab, v_piso_hab
      FROM espacios e JOIN pisos p ON p.id = e.piso_id
     WHERE e.id = NEW.espacio_id
       FOR SHARE OF e, p;  -- serializa con deshabilitar espacio/piso aunque la aplicacion no bloquee
    IF v_tipo_veh IS DISTINCT FROM v_tipo_esp THEN
        RAISE EXCEPTION 'El tipo de vehiculo (%) no coincide con el del espacio (%)', v_tipo_veh, v_tipo_esp
            USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.estado = 'ACTIVA' AND (NOT v_esp_hab OR NOT v_piso_hab) THEN
        RAISE EXCEPTION 'No se puede abrir una sesion en un espacio o piso deshabilitado'
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_sesion_validar_insert BEFORE INSERT ON sesiones_parqueo
    FOR EACH ROW EXECUTE FUNCTION fn_sesion_validar_insert();

-- Sesión cerrada/anulada es definitiva; columnas de identidad inmutables.
CREATE FUNCTION fn_sesion_inmutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.estado <> 'ACTIVA' THEN
        RAISE EXCEPTION 'La sesion % esta % y no puede modificarse', OLD.numero_ticket, OLD.estado
            USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.id <> OLD.id OR NEW.numero_ticket <> OLD.numero_ticket
       OR NEW.vehiculo_id <> OLD.vehiculo_id OR NEW.espacio_id <> OLD.espacio_id
       OR NEW.fecha_entrada <> OLD.fecha_entrada
       OR NEW.operador_entrada_id <> OLD.operador_entrada_id THEN
        RAISE EXCEPTION 'Ticket, vehiculo, espacio, entrada y operador de entrada son inmutables'
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_sesion_inmutable BEFORE UPDATE ON sesiones_parqueo
    FOR EACH ROW EXECUTE FUNCTION fn_sesion_inmutable();

-- RN-P4: no deshabilitar espacio ni piso con sesiones ACTIVAS.
CREATE FUNCTION fn_espacio_deshabilitar() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM sesiones_parqueo WHERE espacio_id = OLD.id AND estado = 'ACTIVA') THEN
        RAISE EXCEPTION 'No se puede deshabilitar el espacio %: tiene una sesion activa', OLD.codigo
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_espacio_deshabilitar BEFORE UPDATE OF habilitado ON espacios
    FOR EACH ROW WHEN (OLD.habilitado AND NOT NEW.habilitado)
    EXECUTE FUNCTION fn_espacio_deshabilitar();

CREATE FUNCTION fn_piso_deshabilitar() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM sesiones_parqueo s JOIN espacios e ON e.id = s.espacio_id
                WHERE e.piso_id = OLD.id AND s.estado = 'ACTIVA') THEN
        RAISE EXCEPTION 'No se puede deshabilitar el piso %: tiene sesiones activas', OLD.codigo
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_piso_deshabilitar BEFORE UPDATE OF habilitado ON pisos
    FOR EACH ROW WHEN (OLD.habilitado AND NOT NEW.habilitado)
    EXECUTE FUNCTION fn_piso_deshabilitar();
