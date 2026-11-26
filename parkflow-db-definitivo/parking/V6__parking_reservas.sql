CREATE SEQUENCE seq_reserva START WITH 1;

CREATE TABLE reservas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo_reserva VARCHAR(30) NOT NULL UNIQUE CHECK (btrim(codigo_reserva) <> ''),
    espacio_id UUID NOT NULL,
    placa VARCHAR(10) NOT NULL CHECK (placa = upper(regexp_replace(btrim(placa), '[[:space:]]+', '', 'g')) AND placa <> ''),
    tipo_vehiculo VARCHAR(10) NOT NULL CHECK (tipo_vehiculo IN ('CARRO','MOTO')),
    fecha_inicio TIMESTAMPTZ NOT NULL,
    fecha_fin TIMESTAMPTZ NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA','CONVERTIDA','CANCELADA','EXPIRADA')),
    sesion_id UUID REFERENCES sesiones_parqueo(id) ON DELETE RESTRICT,
    clave_idempotencia UUID NOT NULL UNIQUE,
    operador_creacion_id UUID NOT NULL,
    operador_cierre_id UUID,
    motivo_cancelacion VARCHAR(255),
    observacion VARCHAR(255),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (espacio_id, tipo_vehiculo) REFERENCES espacios(id, tipo_vehiculo) ON DELETE RESTRICT,
    CHECK (fecha_fin > fecha_inicio),
    CHECK (
        (estado = 'ACTIVA' AND sesion_id IS NULL)
        OR (estado = 'CONVERTIDA' AND sesion_id IS NOT NULL)
        OR (estado IN ('CANCELADA','EXPIRADA'))
    )
);

CREATE UNIQUE INDEX uq_reserva_placa_activa ON reservas(placa) WHERE estado = 'ACTIVA';
CREATE UNIQUE INDEX uq_reserva_sesion_convertida ON reservas(sesion_id) WHERE sesion_id IS NOT NULL;
CREATE INDEX idx_reservas_espacio_estado_ventana ON reservas(espacio_id, fecha_inicio, fecha_fin) WHERE estado = 'ACTIVA';

GRANT SELECT, INSERT, UPDATE ON reservas TO parking_app;
REVOKE DELETE ON reservas FROM parking_app;
GRANT USAGE, SELECT ON SEQUENCE seq_reserva TO parking_app;
