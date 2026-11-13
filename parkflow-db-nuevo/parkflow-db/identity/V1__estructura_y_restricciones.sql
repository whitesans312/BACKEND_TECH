-- ParkFlow / Identity Service / PostgreSQL
CREATE TABLE usuarios (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 nombre VARCHAR(120) NOT NULL CHECK (btrim(nombre) <> ''),
 email VARCHAR(150) NOT NULL,
 password_hash VARCHAR(255) NOT NULL,
 estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO','INACTIVO')),
 intentos_fallidos SMALLINT NOT NULL DEFAULT 0 CHECK (intentos_fallidos >= 0),
 bloqueado_hasta TIMESTAMPTZ,
 ultimo_login TIMESTAMPTZ,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (email = lower(btrim(email))),
 CHECK (email <> '')
);
CREATE UNIQUE INDEX uq_usuarios_email_lower ON usuarios(lower(email));

CREATE TABLE roles (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 nombre VARCHAR(30) NOT NULL UNIQUE CHECK (nombre IN ('ADMINISTRADOR','OPERADOR')),
 descripcion VARCHAR(150),
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE usuario_roles (
 usuario_id UUID NOT NULL REFERENCES usuarios(id),
 rol_id UUID NOT NULL REFERENCES roles(id),
 asignado_por UUID REFERENCES usuarios(id),
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE autorizaciones_excepcionales (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 jti UUID NOT NULL UNIQUE,
 solicitud_parking_id UUID NOT NULL UNIQUE,
 sesion_parking_id UUID NOT NULL,
 operador_solicitante_id UUID NOT NULL REFERENCES usuarios(id),
 administrador_autorizador_id UUID NOT NULL REFERENCES usuarios(id),
 estado VARCHAR(15) NOT NULL CHECK (estado IN ('EMITIDA','CONSUMIDA','REVOCADA','EXPIRADA')),
 finalidad VARCHAR(40) NOT NULL CHECK (finalidad = 'SALIDA_EXCEPCIONAL'),
 motivo TEXT NOT NULL CHECK (btrim(motivo) <> ''),
 emitida_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 expira_en TIMESTAMPTZ NOT NULL,
 consumida_en TIMESTAMPTZ,
 CHECK (expira_en > emitida_en),
 CHECK ((estado = 'CONSUMIDA' AND consumida_en IS NOT NULL) OR (estado <> 'CONSUMIDA' AND consumida_en IS NULL))
);

CREATE TABLE auditoria_seguridad (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 usuario_id UUID REFERENCES usuarios(id),
 evento VARCHAR(40) NOT NULL CHECK (evento IN ('LOGIN_OK','LOGIN_FALLIDO','BLOQUEO','CAMBIO_ROL','USUARIO_CREADO','USUARIO_DESACTIVADO','AUTORIZACION_EMITIDA','AUTORIZACION_CONSUMIDA','AUTORIZACION_REVOCADA')),
 ip_origen INET,
 agente_usuario TEXT,
 detalle JSONB,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
