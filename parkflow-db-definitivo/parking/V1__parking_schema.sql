CREATE TABLE pisos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero SMALLINT NOT NULL UNIQUE CHECK (numero > 0),
    codigo VARCHAR(15) NOT NULL UNIQUE CHECK (btrim(codigo) <> ''),
    nombre VARCHAR(80) NOT NULL CHECK (btrim(nombre) <> ''),
    descripcion TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE zonas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    piso_id UUID NOT NULL REFERENCES pisos(id) ON DELETE RESTRICT,
    codigo VARCHAR(15) NOT NULL CHECK (btrim(codigo) <> ''),
    nombre VARCHAR(80) NOT NULL CHECK (btrim(nombre) <> ''),
    descripcion TEXT,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (piso_id, codigo),
    UNIQUE (id, piso_id)
);

CREATE TABLE espacios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    zona_id UUID NOT NULL REFERENCES zonas(id) ON DELETE RESTRICT,
    codigo VARCHAR(20) NOT NULL CHECK (btrim(codigo) <> ''),
    tipo_vehiculo VARCHAR(10) NOT NULL CHECK (tipo_vehiculo IN ('CARRO','MOTO')),
    estado VARCHAR(20) NOT NULL DEFAULT 'HABILITADO' CHECK (estado IN ('HABILITADO','DESHABILITADO','MANTENIMIENTO')),
    descripcion TEXT,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (zona_id, codigo),
    UNIQUE (id, tipo_vehiculo)
);

CREATE SEQUENCE seq_ticket START WITH 1;

CREATE TABLE sesiones_parqueo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_tiquete VARCHAR(30) NOT NULL UNIQUE,
    espacio_id UUID NOT NULL,
    placa_ingreso VARCHAR(10) NOT NULL CHECK (placa_ingreso = upper(regexp_replace(btrim(placa_ingreso), '[[:space:]]+', '', 'g')) AND placa_ingreso <> ''),
    placa_actual VARCHAR(10) NOT NULL CHECK (placa_actual = upper(regexp_replace(btrim(placa_actual), '[[:space:]]+', '', 'g')) AND placa_actual <> ''),
    tipo_vehiculo VARCHAR(10) NOT NULL CHECK (tipo_vehiculo IN ('CARRO','MOTO')),
    operador_entrada_id UUID NOT NULL,
    operador_salida_id UUID,
    hora_entrada TIMESTAMPTZ NOT NULL DEFAULT now(),
    hora_salida TIMESTAMPTZ,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA','CERRADA','ANULADA')),
    tipo_cierre VARCHAR(15) CHECK (tipo_cierre IN ('PAGADA','GRATUITA','EXCEPCIONAL','ABANDONO')),
    liquidacion_id UUID,
    operacion_cierre_id UUID UNIQUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (espacio_id, tipo_vehiculo) REFERENCES espacios(id, tipo_vehiculo) ON DELETE RESTRICT,
    CHECK (hora_salida IS NULL OR hora_salida >= hora_entrada),
    CHECK (
        (estado = 'ACTIVA' AND hora_salida IS NULL AND tipo_cierre IS NULL AND operador_salida_id IS NULL AND liquidacion_id IS NULL AND operacion_cierre_id IS NULL)
        OR (estado = 'CERRADA' AND hora_salida IS NOT NULL AND operador_salida_id IS NOT NULL AND tipo_cierre IS NOT NULL AND operacion_cierre_id IS NOT NULL)
        OR (estado = 'ANULADA' AND hora_salida IS NULL AND tipo_cierre IS NULL AND liquidacion_id IS NULL)
    ),
    CHECK (tipo_cierre IS DISTINCT FROM 'PAGADA' OR liquidacion_id IS NOT NULL),
    CHECK (tipo_cierre IS DISTINCT FROM 'GRATUITA' OR liquidacion_id IS NOT NULL)
);

CREATE TABLE anulaciones_sesion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sesion_id UUID NOT NULL UNIQUE REFERENCES sesiones_parqueo(id) ON DELETE RESTRICT,
    administrador_id UUID NOT NULL,
    motivo TEXT NOT NULL CHECK (btrim(motivo) <> ''),
    clave_idempotencia UUID NOT NULL UNIQUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE solicitudes_salida_excepcional (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sesion_id UUID NOT NULL REFERENCES sesiones_parqueo(id) ON DELETE RESTRICT,
    operador_solicitante_id UUID NOT NULL,
    motivo TEXT NOT NULL CHECK (btrim(motivo) <> ''),
    estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','AUTORIZADA','RECHAZADA','CANCELADA','EXPIRADA')),
    administrador_resolutor_id UUID,
    autorizacion_identity_id UUID UNIQUE,
    operacion_id UUID NOT NULL UNIQUE,
    jti UUID UNIQUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    resuelta_en TIMESTAMPTZ,
    CHECK ((estado = 'PENDIENTE' AND resuelta_en IS NULL) OR (estado <> 'PENDIENTE' AND resuelta_en IS NOT NULL))
);

CREATE TABLE salidas_excepcionales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sesion_id UUID NOT NULL UNIQUE REFERENCES sesiones_parqueo(id) ON DELETE RESTRICT,
    solicitud_id UUID NOT NULL UNIQUE REFERENCES solicitudes_salida_excepcional(id) ON DELETE RESTRICT,
    operacion_id UUID NOT NULL UNIQUE,
    autorizacion_identity_id UUID NOT NULL UNIQUE,
    jti UUID NOT NULL UNIQUE,
    operador_ejecutor_id UUID NOT NULL,
    administrador_autorizador_id UUID NOT NULL,
    motivo TEXT NOT NULL CHECK (btrim(motivo) <> ''),
    ejecutada_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE mantenimientos_espacio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    espacio_id UUID NOT NULL REFERENCES espacios(id) ON DELETE RESTRICT,
    operador_responsable_id UUID NOT NULL,
    administrador_id UUID,
    motivo VARCHAR(20) NOT NULL CHECK (motivo IN ('DANO','LIMPIEZA','REPARACION','REVISION','OTRO')),
    descripcion TEXT NOT NULL CHECK (btrim(descripcion) <> ''),
    estado VARCHAR(15) NOT NULL DEFAULT 'EN_PROCESO' CHECK (estado IN ('EN_PROCESO','FINALIZADO','CANCELADO')),
    iniciado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    finalizado_en TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (finalizado_en IS NULL OR finalizado_en >= iniciado_en),
    CHECK ((estado = 'EN_PROCESO' AND finalizado_en IS NULL) OR (estado <> 'EN_PROCESO' AND finalizado_en IS NOT NULL))
);

CREATE TABLE eventos_operativos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sesion_id UUID REFERENCES sesiones_parqueo(id) ON DELETE RESTRICT,
    operador_id UUID NOT NULL,
    tipo_evento VARCHAR(40) NOT NULL,
    motivo TEXT,
    datos_anteriores JSONB,
    datos_nuevos JSONB,
    clave_idempotencia UUID UNIQUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE traslados_espacio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sesion_id UUID NOT NULL REFERENCES sesiones_parqueo(id) ON DELETE RESTRICT,
    espacio_origen_id UUID NOT NULL REFERENCES espacios(id) ON DELETE RESTRICT,
    espacio_destino_id UUID NOT NULL REFERENCES espacios(id) ON DELETE RESTRICT,
    operador_id UUID NOT NULL,
    motivo TEXT NOT NULL CHECK (btrim(motivo) <> ''),
    trasladado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (espacio_origen_id <> espacio_destino_id)
);

CREATE TABLE casos_abandono (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sesion_id UUID NOT NULL UNIQUE REFERENCES sesiones_parqueo(id) ON DELETE RESTRICT,
    estado VARCHAR(30) NOT NULL DEFAULT 'ABANDONO_REPORTADO' CHECK (estado IN ('ABANDONO_REPORTADO','EN_GESTION','AUTORIDAD_CONTACTADA','RETIRO_COORDINADO','VEHICULO_RETIRADO','CERRADO')),
    reportado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    operador_reporta_id UUID NOT NULL,
    motivo TEXT NOT NULL CHECK (btrim(motivo) <> ''),
    circunstancias JSONB,
    cargo_billing_id UUID,
    autorizacion_identity_id UUID,
    operacion_cierre_id UUID UNIQUE,
    cerrado_en TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((estado = 'CERRADO' AND cerrado_en IS NOT NULL) OR (estado <> 'CERRADO' AND cerrado_en IS NULL))
);

CREATE TABLE contactos_autoridad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caso_abandono_id UUID NOT NULL REFERENCES casos_abandono(id) ON DELETE RESTRICT,
    institucion VARCHAR(100) NOT NULL,
    contactado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    operador_id UUID NOT NULL,
    canal VARCHAR(30),
    referencia VARCHAR(100),
    resultado TEXT
);

CREATE TABLE evidencias_abandono (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caso_abandono_id UUID NOT NULL REFERENCES casos_abandono(id) ON DELETE RESTRICT,
    tipo VARCHAR(30) NOT NULL,
    descripcion TEXT,
    referencia_archivo VARCHAR(500),
    checksum CHAR(64),
    registrado_por UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE retiros_vehiculo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caso_abandono_id UUID NOT NULL UNIQUE REFERENCES casos_abandono(id) ON DELETE RESTRICT,
    retirado_en TIMESTAMPTZ NOT NULL,
    responsable_retiro VARCHAR(150) NOT NULL,
    tipo_responsable VARCHAR(30) NOT NULL,
    operador_entrega_id UUID NOT NULL,
    observaciones TEXT,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
