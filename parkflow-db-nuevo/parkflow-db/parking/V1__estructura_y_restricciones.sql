-- ParkFlow / Parking Service / PostgreSQL
CREATE TABLE pisos (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 numero SMALLINT NOT NULL UNIQUE CHECK (numero >= -5),
 codigo VARCHAR(15) NOT NULL UNIQUE CHECK (btrim(codigo) <> ''),
 nombre VARCHAR(80) NOT NULL CHECK (btrim(nombre) <> ''),
 descripcion TEXT,
 activo BOOLEAN NOT NULL DEFAULT TRUE,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE zonas (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 piso_id UUID NOT NULL REFERENCES pisos(id),
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
 zona_id UUID NOT NULL REFERENCES zonas(id),
 codigo VARCHAR(20) NOT NULL CHECK (btrim(codigo) <> ''),
 tipo_vehiculo VARCHAR(10) NOT NULL CHECK (tipo_vehiculo IN ('CARRO','MOTO')),
 estado VARCHAR(20) NOT NULL DEFAULT 'HABILITADO' CHECK (estado IN ('HABILITADO','DESHABILITADO','MANTENIMIENTO')),
 descripcion TEXT,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (zona_id, codigo),
 UNIQUE (id, tipo_vehiculo)
);

CREATE TABLE vehiculos (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 placa VARCHAR(10) NOT NULL,
 tipo_vehiculo VARCHAR(10) NOT NULL CHECK (tipo_vehiculo IN ('CARRO','MOTO')),
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (placa),
 UNIQUE (id, tipo_vehiculo),
 CHECK (placa = upper(regexp_replace(btrim(placa), '[[:space:]]+', '', 'g'))),
 CHECK (placa <> '')
);

CREATE SEQUENCE seq_ticket START 1;
CREATE TABLE sesiones_parqueo (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 numero_tiquete VARCHAR(30) NOT NULL UNIQUE,
 vehiculo_id UUID NOT NULL,
 espacio_id UUID NOT NULL,
 tipo_vehiculo VARCHAR(10) NOT NULL CHECK (tipo_vehiculo IN ('CARRO','MOTO')),
 operador_entrada_id UUID NOT NULL,
 operador_salida_id UUID,
 hora_entrada TIMESTAMPTZ NOT NULL DEFAULT now(),
 hora_salida TIMESTAMPTZ,
 estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA','CERRADA','ANULADA')),
 tipo_cierre VARCHAR(15) CHECK (tipo_cierre IN ('PAGADA','EXCEPCIONAL')),
 pago_id UUID,
 anulacion_id UUID,
 salida_excepcional_id UUID,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 FOREIGN KEY (vehiculo_id, tipo_vehiculo) REFERENCES vehiculos(id, tipo_vehiculo),
 FOREIGN KEY (espacio_id, tipo_vehiculo) REFERENCES espacios(id, tipo_vehiculo),
 CHECK (hora_salida IS NULL OR hora_salida >= hora_entrada),
 CHECK (
  (estado = 'ACTIVA' AND hora_salida IS NULL AND tipo_cierre IS NULL AND pago_id IS NULL AND anulacion_id IS NULL AND salida_excepcional_id IS NULL AND operador_salida_id IS NULL)
  OR (estado = 'CERRADA' AND hora_salida IS NOT NULL AND operador_salida_id IS NOT NULL AND ((tipo_cierre = 'PAGADA' AND pago_id IS NOT NULL AND salida_excepcional_id IS NULL AND anulacion_id IS NULL) OR (tipo_cierre = 'EXCEPCIONAL' AND pago_id IS NULL AND salida_excepcional_id IS NOT NULL AND anulacion_id IS NULL)))
  OR (estado = 'ANULADA' AND hora_salida IS NULL AND tipo_cierre IS NULL AND pago_id IS NULL AND anulacion_id IS NOT NULL AND salida_excepcional_id IS NULL)
 )
);

CREATE TABLE anulaciones_sesion (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 sesion_id UUID NOT NULL UNIQUE REFERENCES sesiones_parqueo(id) DEFERRABLE INITIALLY DEFERRED,
 administrador_id UUID NOT NULL,
 motivo TEXT NOT NULL CHECK (btrim(motivo) <> ''),
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE sesiones_parqueo ADD CONSTRAINT fk_sesion_anulacion FOREIGN KEY (anulacion_id) REFERENCES anulaciones_sesion(id) DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE solicitudes_salida_excepcional (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 sesion_id UUID NOT NULL REFERENCES sesiones_parqueo(id),
 operador_solicitante_id UUID NOT NULL,
 motivo TEXT NOT NULL CHECK (btrim(motivo) <> ''),
 estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','AUTORIZADA','RECHAZADA','CANCELADA','EXPIRADA')),
 administrador_resolutor_id UUID,
 jti_autorizacion UUID,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 resuelta_en TIMESTAMPTZ,
 CHECK ((estado = 'PENDIENTE' AND resuelta_en IS NULL AND administrador_resolutor_id IS NULL) OR (estado <> 'PENDIENTE' AND resuelta_en IS NOT NULL))
);

CREATE TABLE salidas_excepcionales (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 sesion_id UUID NOT NULL UNIQUE REFERENCES sesiones_parqueo(id) DEFERRABLE INITIALLY DEFERRED,
 solicitud_id UUID NOT NULL UNIQUE REFERENCES solicitudes_salida_excepcional(id),
 operador_ejecutor_id UUID NOT NULL,
 administrador_autorizador_id UUID NOT NULL,
 jti_autorizacion UUID NOT NULL UNIQUE,
 motivo TEXT NOT NULL CHECK (btrim(motivo) <> ''),
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE sesiones_parqueo ADD CONSTRAINT fk_sesion_salida_excepcional FOREIGN KEY (salida_excepcional_id) REFERENCES salidas_excepcionales(id) DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE mantenimientos_espacio (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 espacio_id UUID NOT NULL REFERENCES espacios(id),
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

-- Bitácora operativa append-only: correcciones de placa, pérdida de tiquete,
-- cambios de espacio y otras acciones que deben quedar trazables.
CREATE TABLE eventos_operativos (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 sesion_id UUID REFERENCES sesiones_parqueo(id),
 vehiculo_id UUID REFERENCES vehiculos(id),
 operador_id UUID NOT NULL,
 tipo_evento VARCHAR(40) NOT NULL,
 motivo TEXT,
 datos_anteriores JSONB,
 datos_nuevos JSONB,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE traslados_espacio (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 sesion_id UUID NOT NULL REFERENCES sesiones_parqueo(id),
 espacio_origen_id UUID NOT NULL REFERENCES espacios(id),
 espacio_destino_id UUID NOT NULL REFERENCES espacios(id),
 operador_id UUID NOT NULL,
 motivo TEXT NOT NULL CHECK (btrim(motivo) <> ''),
 trasladado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (espacio_origen_id <> espacio_destino_id)
);
