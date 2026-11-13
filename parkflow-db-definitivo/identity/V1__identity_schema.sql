CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(120) NOT NULL CHECK (btrim(nombre) <> ''),
    email VARCHAR(150) NOT NULL CHECK (email = lower(btrim(email)) AND email <> ''),
    password_hash VARCHAR(255) NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO','INACTIVO')),
    intentos_fallidos SMALLINT NOT NULL DEFAULT 0 CHECK (intentos_fallidos >= 0),
    bloqueado_hasta TIMESTAMPTZ,
    ultimo_login TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE identity_bootstrap_lock (
    id SMALLINT PRIMARY KEY CHECK (id = 1)
);
INSERT INTO identity_bootstrap_lock(id) VALUES (1);
CREATE UNIQUE INDEX uq_usuarios_email_lower ON usuarios(lower(email));

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(30) NOT NULL UNIQUE CHECK (nombre IN ('ADMINISTRADOR','OPERADOR')),
    descripcion VARCHAR(150),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE usuario_roles (
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    rol_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    asignado_por UUID REFERENCES usuarios(id) ON DELETE RESTRICT,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE autorizaciones_excepcionales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    jti UUID NOT NULL UNIQUE,
    solicitud_parking_id UUID NOT NULL UNIQUE,
    sesion_parking_id UUID NOT NULL,
    operacion_id UUID NOT NULL UNIQUE,
    operador_solicitante_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    administrador_autorizador_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    roles_verificados JSONB NOT NULL,
    estado VARCHAR(15) NOT NULL CHECK (estado IN ('EMITIDA','CONSUMIDA','REVOCADA','EXPIRADA')),
    finalidad VARCHAR(40) NOT NULL CHECK (finalidad = 'SALIDA_EXCEPCIONAL'),
    motivo TEXT NOT NULL CHECK (btrim(motivo) <> ''),
    emitida_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_en TIMESTAMPTZ NOT NULL,
    consumida_en TIMESTAMPTZ,
    resultado_consumo JSONB,
    CHECK (expira_en > emitida_en),
    CHECK ((estado = 'CONSUMIDA' AND consumida_en IS NOT NULL) OR (estado <> 'CONSUMIDA' AND consumida_en IS NULL))
);

CREATE TABLE autorizaciones_cargo_abandono (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caso_abandono_id UUID NOT NULL,
    operacion_id UUID NOT NULL UNIQUE,
    operador_solicitante_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    administrador_autorizador_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    importe_autorizado NUMERIC(12,2) NOT NULL CHECK (importe_autorizado > 0),
    moneda CHAR(3) NOT NULL DEFAULT 'COP' CHECK (moneda = 'COP'),
    motivo TEXT NOT NULL CHECK (btrim(motivo) <> ''),
    roles_verificados JSONB NOT NULL,
    estado VARCHAR(15) NOT NULL CHECK (estado IN ('AUTORIZADA','REVOCADA')),
    autorizada_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    revocada_en TIMESTAMPTZ,
    clave_idempotencia UUID NOT NULL UNIQUE,
    CHECK ((estado = 'AUTORIZADA' AND revocada_en IS NULL) OR (estado = 'REVOCADA' AND revocada_en IS NOT NULL))
);

CREATE TABLE auditoria_seguridad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID REFERENCES usuarios(id) ON DELETE RESTRICT,
    evento VARCHAR(50) NOT NULL,
    ip_origen INET,
    agente_usuario TEXT,
    detalle JSONB,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
