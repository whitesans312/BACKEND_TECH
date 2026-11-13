-- ParkFlow / Billing Service / PostgreSQL
CREATE TABLE parametros_facturacion (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 clave VARCHAR(50) NOT NULL UNIQUE,
 valor VARCHAR(100) NOT NULL,
 descripcion TEXT,
 vigente_desde TIMESTAMPTZ NOT NULL DEFAULT now(),
 vigente_hasta TIMESTAMPTZ,
 creado_por UUID,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (vigente_hasta IS NULL OR vigente_hasta > vigente_desde)
);

-- Un registro por tipo de vehículo, concepto tarifario y vigencia.
-- Los importes son datos configurables; el motor de cálculo vive en Java.
CREATE TABLE tarifas (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 tipo_vehiculo VARCHAR(10) NOT NULL CHECK (tipo_vehiculo IN ('CARRO','MOTO')),
 concepto VARCHAR(25) NOT NULL CHECK (concepto IN ('HORA','DIA','RECARGO_NOCTURNO')),
 valor NUMERIC(12,2) NOT NULL CHECK (valor >= 0),
 moneda CHAR(3) NOT NULL DEFAULT 'COP' CHECK (moneda = 'COP'),
 vigente_desde TIMESTAMPTZ NOT NULL,
 vigente_hasta TIMESTAMPTZ,
 activa BOOLEAN NOT NULL DEFAULT TRUE,
 creado_por UUID,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (vigente_hasta IS NULL OR vigente_hasta > vigente_desde),
 UNIQUE (id, tipo_vehiculo, concepto, valor)
);

CREATE TABLE liquidaciones (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 sesion_parqueo_id UUID NOT NULL,
 numero_tiquete VARCHAR(30) NOT NULL,
 placa VARCHAR(10) NOT NULL,
 tipo_vehiculo VARCHAR(10) NOT NULL CHECK (tipo_vehiculo IN ('CARRO','MOTO')),
 hora_entrada TIMESTAMPTZ NOT NULL,
 hora_corte TIMESTAMPTZ NOT NULL,
 minutos_totales INTEGER NOT NULL CHECK (minutos_totales >= 0),
 minutos_gracia SMALLINT NOT NULL CHECK (minutos_gracia >= 0),
 minutos_facturables INTEGER NOT NULL CHECK (minutos_facturables >= 0),
 subtotal NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
 recargos NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (recargos >= 0),
 descuentos NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (descuentos >= 0),
 total NUMERIC(12,2) NOT NULL CHECK (total >= 0),
 estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','PAGADA','ANULADA','CANCELADA')),
 clave_idempotencia UUID NOT NULL UNIQUE,
 hash_solicitud CHAR(64) NOT NULL,
 operador_liquida_id UUID NOT NULL,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (hora_corte >= hora_entrada),
 CHECK (total = subtotal + recargos - descuentos),
 UNIQUE (id, total)
);

CREATE TABLE detalles_liquidacion (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 liquidacion_id UUID NOT NULL REFERENCES liquidaciones(id),
 tarifa_id UUID REFERENCES tarifas(id),
 concepto VARCHAR(30) NOT NULL CHECK (concepto IN ('TIEMPO_GRATIS','COBRO_HORAS','TARIFA_DIARIA','RECARGO_NOCTURNO','DESCUENTO','AJUSTE')),
 descripcion VARCHAR(200) NOT NULL,
 cantidad NUMERIC(10,2) NOT NULL DEFAULT 1 CHECK (cantidad >= 0),
 valor_unitario NUMERIC(12,2) NOT NULL CHECK (valor_unitario >= 0),
 subtotal NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
 datos_calculo JSONB,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cajas (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 codigo VARCHAR(20) NOT NULL UNIQUE CHECK (btrim(codigo) <> ''),
 nombre VARCHAR(80) NOT NULL CHECK (btrim(nombre) <> ''),
 descripcion TEXT,
 activa BOOLEAN NOT NULL DEFAULT TRUE,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sesiones_caja (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 caja_id UUID NOT NULL REFERENCES cajas(id),
 operador_id UUID NOT NULL,
 estado VARCHAR(10) NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA','CERRADA')),
 saldo_inicial NUMERIC(12,2) NOT NULL CHECK (saldo_inicial >= 0),
 saldo_esperado NUMERIC(12,2) NOT NULL CHECK (saldo_esperado >= 0),
 saldo_contado NUMERIC(12,2),
 diferencia NUMERIC(12,2),
 abierta_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 cerrada_en TIMESTAMPTZ,
 observacion_apertura TEXT,
 observacion_cierre TEXT,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK ((estado = 'ABIERTA' AND cerrada_en IS NULL AND saldo_contado IS NULL AND diferencia IS NULL) OR (estado = 'CERRADA' AND cerrada_en IS NOT NULL AND saldo_contado IS NOT NULL AND diferencia IS NOT NULL)),
 CHECK (cerrada_en IS NULL OR cerrada_en >= abierta_en)
);

CREATE TABLE pagos (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 liquidacion_id UUID NOT NULL REFERENCES liquidaciones(id),
 monto NUMERIC(12,2) NOT NULL CHECK (monto > 0),
 metodo VARCHAR(15) NOT NULL DEFAULT 'EFECTIVO' CHECK (metodo = 'EFECTIVO'),
 estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','APROBADO','RECHAZADO')),
 operador_id UUID NOT NULL,
 sesion_caja_id UUID REFERENCES sesiones_caja(id),
 clave_idempotencia UUID NOT NULL UNIQUE,
 aprobado_en TIMESTAMPTZ,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (id, liquidacion_id),
 CHECK ((estado = 'APROBADO' AND aprobado_en IS NOT NULL AND sesion_caja_id IS NOT NULL) OR (estado <> 'APROBADO' AND aprobado_en IS NULL))
);

CREATE TABLE movimientos_caja (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 sesion_caja_id UUID NOT NULL REFERENCES sesiones_caja(id),
 pago_id UUID UNIQUE REFERENCES pagos(id),
 operador_id UUID NOT NULL,
 tipo_movimiento VARCHAR(10) NOT NULL CHECK (tipo_movimiento IN ('INGRESO','EGRESO')),
 concepto VARCHAR(25) NOT NULL CHECK (concepto IN ('PAGO_PARQUEO','RETIRO','AJUSTE_INGRESO','AJUSTE_EGRESO')),
 monto NUMERIC(12,2) NOT NULL CHECK (monto > 0),
 referencia VARCHAR(100),
 motivo TEXT,
 autorizado_por UUID,
 clave_idempotencia UUID NOT NULL UNIQUE,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK ((concepto = 'PAGO_PARQUEO' AND tipo_movimiento = 'INGRESO' AND pago_id IS NOT NULL) OR (concepto <> 'PAGO_PARQUEO' AND pago_id IS NULL)),
 CHECK ((concepto IN ('RETIRO','AJUSTE_INGRESO','AJUSTE_EGRESO') AND motivo IS NOT NULL AND btrim(motivo) <> '' AND autorizado_por IS NOT NULL) OR concepto = 'PAGO_PARQUEO')
);

CREATE TABLE comprobantes (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 pago_id UUID NOT NULL UNIQUE REFERENCES pagos(id),
 liquidacion_id UUID NOT NULL REFERENCES liquidaciones(id),
 numero VARCHAR(30) NOT NULL UNIQUE,
 numero_tiquete VARCHAR(30) NOT NULL,
 placa VARCHAR(10) NOT NULL,
 tipo_vehiculo VARCHAR(10) NOT NULL CHECK (tipo_vehiculo IN ('CARRO','MOTO')),
 metodo_pago VARCHAR(15) NOT NULL CHECK (metodo_pago = 'EFECTIVO'),
 hora_entrada TIMESTAMPTZ NOT NULL,
 hora_corte TIMESTAMPTZ NOT NULL,
 subtotal NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
 recargos NUMERIC(12,2) NOT NULL CHECK (recargos >= 0),
 descuentos NUMERIC(12,2) NOT NULL CHECK (descuentos >= 0),
 total NUMERIC(12,2) NOT NULL CHECK (total > 0),
 emitido_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (hora_corte >= hora_entrada),
 UNIQUE (id, pago_id)
);

CREATE TABLE anulaciones_liquidacion (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 liquidacion_id UUID NOT NULL UNIQUE REFERENCES liquidaciones(id),
 administrador_id UUID NOT NULL,
 motivo TEXT NOT NULL CHECK (btrim(motivo) <> ''),
 clave_idempotencia UUID NOT NULL UNIQUE,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
