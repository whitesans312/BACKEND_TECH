-- ERG-POS: instalación de una base nueva e independiente en Supabase.
-- Fuente estructural: SQL extraído de Supabase compartido en la conversación.
-- Funciones, secuencias, vistas y datos de catálogo complementarios:
-- DATABASE_SCHEMA.md. No incluye usuarios ni datos operativos.
-- Ejecutar una sola vez en una base vacía; no es una migración ni un reset.
-- Las funciones se reconstruyen desde DATABASE_SCHEMA.md porque el extracto
-- estructural no incluía sus definiciones. Este archivo no ha sido ejecutado
-- ni contrastado en una instancia de Supabase.
-- No se crean usuarios iniciales. Con la base instalada, cree el primer
-- administrador una sola vez mediante POST /api/auth/bootstrap, enviando el
-- encabezado X-Bootstrap-Token configurado en BOOTSTRAP_TOKEN; el backend
-- asigna el rol ADMIN y cifra la contraseña. No inserte contraseñas en texto plano.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Secuencias exclusivas de esta base. La primera factura de cada tipo será 0001.
CREATE SEQUENCE public.seq_facturas_orden START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE public.seq_facturas_venta START WITH 1 INCREMENT BY 1 NO CYCLE;
ALTER SEQUENCE public.seq_facturas_orden RESTART WITH 1;
ALTER SEQUENCE public.seq_facturas_venta RESTART WITH 1;

CREATE OR REPLACE FUNCTION public.fn_set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at := CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_actualizar_stock()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.tipo = 'ENTRADA' THEN
    UPDATE public.productos
       SET stock = stock + NEW.cantidad
     WHERE id = NEW.producto_id;
  ELSIF NEW.tipo = 'SALIDA' THEN
    UPDATE public.productos
       SET stock = stock - NEW.cantidad
     WHERE id = NEW.producto_id
       AND stock >= NEW.cantidad;

    IF NOT FOUND THEN
      RAISE EXCEPTION
        'Stock insuficiente para el producto %. Disponible: %, requerido: %',
        NEW.producto_id,
        (SELECT stock FROM public.productos WHERE id = NEW.producto_id),
        NEW.cantidad;
    END IF;
  END IF;
  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_auditoria_bloquear_modificaciones()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  RAISE EXCEPTION 'La tabla auditoria es inmutable: use INSERT para registrar nuevos eventos';
END;
$$;

-- Catálogos referenciados por las tablas operativas.
CREATE TABLE public.catalogo_acciones_dinero_devolucion (
  codigo varchar(20) NOT NULL,
  descripcion varchar(80) NOT NULL,
  CONSTRAINT catalogo_acciones_dinero_devolucion_pkey PRIMARY KEY (codigo)
);

CREATE TABLE public.catalogo_estados_caja (
  codigo varchar(20) NOT NULL,
  descripcion varchar(80) NOT NULL,
  orden_visual smallint NOT NULL DEFAULT 0,
  CONSTRAINT catalogo_estados_caja_pkey PRIMARY KEY (codigo)
);

CREATE TABLE public.catalogo_estados_compra (
  codigo varchar(20) NOT NULL,
  descripcion varchar(80) NOT NULL,
  orden_visual smallint NOT NULL DEFAULT 0,
  CONSTRAINT catalogo_estados_compra_pkey PRIMARY KEY (codigo)
);

CREATE TABLE public.catalogo_estados_devolucion (
  codigo varchar(20) NOT NULL,
  descripcion varchar(80) NOT NULL,
  orden_visual smallint NOT NULL DEFAULT 0,
  CONSTRAINT catalogo_estados_devolucion_pkey PRIMARY KEY (codigo)
);

CREATE TABLE public.catalogo_estados_orden (
  codigo varchar(20) NOT NULL,
  descripcion varchar(80) NOT NULL,
  orden_visual smallint NOT NULL DEFAULT 0,
  CONSTRAINT catalogo_estados_orden_pkey PRIMARY KEY (codigo)
);

CREATE TABLE public.catalogo_estados_pago (
  codigo varchar(20) NOT NULL,
  descripcion varchar(80) NOT NULL,
  orden_visual smallint NOT NULL DEFAULT 0,
  CONSTRAINT catalogo_estados_pago_pkey PRIMARY KEY (codigo)
);

CREATE TABLE public.catalogo_estados_venta (
  codigo varchar(20) NOT NULL,
  descripcion varchar(80) NOT NULL,
  orden_visual smallint NOT NULL DEFAULT 0,
  CONSTRAINT catalogo_estados_venta_pkey PRIMARY KEY (codigo)
);

CREATE TABLE public.catalogo_tipos_devolucion (
  codigo varchar(20) NOT NULL,
  descripcion varchar(80) NOT NULL,
  CONSTRAINT catalogo_tipos_devolucion_pkey PRIMARY KEY (codigo)
);

CREATE TABLE public.catalogo_tipos_entrega (
  codigo varchar(20) NOT NULL,
  descripcion varchar(80) NOT NULL,
  CONSTRAINT catalogo_tipos_entrega_pkey PRIMARY KEY (codigo)
);

CREATE TABLE public.catalogo_tipos_movimiento (
  codigo varchar(10) NOT NULL,
  descripcion varchar(80) NOT NULL,
  CONSTRAINT catalogo_tipos_movimiento_pkey PRIMARY KEY (codigo)
);

CREATE TABLE public.catalogo_tipos_movimiento_caja (
  codigo varchar(20) NOT NULL,
  descripcion varchar(80) NOT NULL,
  afecta_saldo varchar(10) NOT NULL,
  orden_visual smallint NOT NULL DEFAULT 0,
  CONSTRAINT catalogo_tipos_movimiento_caja_pkey PRIMARY KEY (codigo),
  CONSTRAINT catalogo_tipos_movimiento_caja_afecta_check
    CHECK (afecta_saldo IN ('INGRESO', 'EGRESO', 'NEUTRO'))
);

-- Tablas base y principales.
CREATE TABLE public.categorias (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  nombre varchar(60) NOT NULL,
  descripcion varchar(120),
  activo boolean NOT NULL DEFAULT true,
  CONSTRAINT categorias_pkey PRIMARY KEY (id),
  CONSTRAINT categorias_nombre_key UNIQUE (nombre)
);

CREATE TABLE public.clientes (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  nombre varchar(120) NOT NULL,
  documento varchar(30),
  telefono varchar(20),
  email varchar(120),
  direccion varchar(200),
  barrio varchar(80),
  ciudad varchar(80) NOT NULL DEFAULT 'Cali',
  notas text,
  activo boolean NOT NULL DEFAULT true,
  created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT clientes_pkey PRIMARY KEY (id),
  CONSTRAINT clientes_email_key UNIQUE (email),
  CONSTRAINT clientes_telefono_key UNIQUE (telefono)
);

CREATE TABLE public.configuracion_negocio (
  clave varchar(50) NOT NULL,
  valor text NOT NULL,
  categoria varchar(30) NOT NULL,
  descripcion varchar(255),
  updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT configuracion_negocio_pkey PRIMARY KEY (clave)
);

CREATE TABLE public.proveedores (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  nombre varchar(120) NOT NULL,
  nit varchar(30),
  telefono varchar(20),
  email varchar(120),
  direccion varchar(200),
  ciudad varchar(80) NOT NULL DEFAULT 'Cali',
  contacto varchar(80),
  notas text,
  activo boolean NOT NULL DEFAULT true,
  created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT proveedores_pkey PRIMARY KEY (id),
  CONSTRAINT proveedores_nit_key UNIQUE (nit)
);

CREATE TABLE public.roles (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  nombre varchar(50) NOT NULL,
  activo boolean NOT NULL DEFAULT true,
  created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT roles_pkey PRIMARY KEY (id),
  CONSTRAINT roles_nombre_key UNIQUE (nombre)
);

CREATE TABLE public.productos (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  codigo varchar(30) NOT NULL,
  nombre varchar(120) NOT NULL,
  descripcion varchar(255),
  categoria_id uuid,
  precio numeric(10,2) NOT NULL,
  stock integer NOT NULL DEFAULT 0,
  stock_minimo integer NOT NULL DEFAULT 5,
  activo boolean NOT NULL DEFAULT true,
  created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT productos_pkey PRIMARY KEY (id),
  CONSTRAINT productos_codigo_key UNIQUE (codigo),
  CONSTRAINT productos_categoria_fkey FOREIGN KEY (categoria_id)
    REFERENCES public.categorias(id) ON DELETE SET NULL,
  CONSTRAINT productos_precio_check CHECK (precio >= 0),
  CONSTRAINT productos_stock_check CHECK (stock >= 0),
  CONSTRAINT productos_stock_minimo_check CHECK (stock_minimo >= 0)
);

CREATE TABLE public.usuarios (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  nombre varchar(120) NOT NULL,
  email varchar(120) NOT NULL,
  password varchar(255) NOT NULL,
  telefono varchar(20),
  activo boolean NOT NULL DEFAULT true,
  rol_id uuid NOT NULL,
  created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT usuarios_pkey PRIMARY KEY (id),
  CONSTRAINT usuarios_email_key UNIQUE (email),
  CONSTRAINT usuarios_rol_id_fkey FOREIGN KEY (rol_id)
    REFERENCES public.roles(id) ON DELETE RESTRICT
);

CREATE TABLE public.auditoria (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  usuario_id uuid,
  usuario_nombre varchar(120),
  accion varchar(60) NOT NULL,
  modulo varchar(30) NOT NULL,
  entidad_id uuid,
  detalle text,
  fecha timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  entidad_tipo varchar(40),
  resultado varchar(20) NOT NULL DEFAULT 'EXITOSO',
  severidad varchar(10) NOT NULL DEFAULT 'INFO',
  ip_origen varchar(45),
  user_agent varchar(255),
  metodo_http varchar(10),
  ruta varchar(255),
  estado_anterior jsonb,
  estado_nuevo jsonb,
  metadata jsonb,
  CONSTRAINT auditoria_pkey PRIMARY KEY (id),
  CONSTRAINT auditoria_usuario_fk FOREIGN KEY (usuario_id)
    REFERENCES public.usuarios(id) ON DELETE SET NULL,
  CONSTRAINT auditoria_resultado_check CHECK (resultado IN ('EXITOSO', 'FALLIDO', 'DENEGADO')),
  CONSTRAINT auditoria_severidad_check CHECK (severidad IN ('INFO', 'WARN', 'ERROR', 'CRITICO'))
);

CREATE TABLE public.cajas (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  usuario_id uuid NOT NULL,
  monto_inicial numeric(10,2) NOT NULL DEFAULT 0,
  monto_final numeric(10,2),
  estado varchar(20) NOT NULL DEFAULT 'ABIERTA',
  fecha_apertura timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_cierre timestamp without time zone,
  notas text,
  created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT cajas_pkey PRIMARY KEY (id),
  CONSTRAINT cajas_estado_fkey FOREIGN KEY (estado)
    REFERENCES public.catalogo_estados_caja(codigo) ON DELETE RESTRICT,
  CONSTRAINT cajas_usuario_fkey FOREIGN KEY (usuario_id)
    REFERENCES public.usuarios(id) ON DELETE RESTRICT,
  CONSTRAINT cajas_fecha_cierre_check CHECK (fecha_cierre IS NULL OR fecha_cierre >= fecha_apertura),
  CONSTRAINT cajas_monto_final_check CHECK (monto_final IS NULL OR monto_final >= 0),
  CONSTRAINT cajas_monto_inicial_check CHECK (monto_inicial >= 0)
);

CREATE TABLE public.compras (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  proveedor_id uuid NOT NULL,
  comprador_id uuid,
  numero_factura varchar(40),
  estado varchar(20) NOT NULL DEFAULT 'PENDIENTE',
  total numeric(10,2) NOT NULL DEFAULT 0,
  notas text,
  fecha timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT compras_pkey PRIMARY KEY (id),
  CONSTRAINT compras_comprador_fkey FOREIGN KEY (comprador_id)
    REFERENCES public.usuarios(id) ON DELETE SET NULL,
  CONSTRAINT compras_estado_fkey FOREIGN KEY (estado)
    REFERENCES public.catalogo_estados_compra(codigo) ON DELETE RESTRICT,
  CONSTRAINT compras_proveedor_fkey FOREIGN KEY (proveedor_id)
    REFERENCES public.proveedores(id) ON DELETE RESTRICT,
  CONSTRAINT compras_total_check CHECK (total >= 0)
);

CREATE TABLE public.entregas (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  cliente_nombre varchar(120) NOT NULL,
  cliente_telefono varchar(20),
  cliente_id uuid,
  tipo varchar(20) NOT NULL DEFAULT 'REPARACION',
  descripcion_problema text,
  direccion varchar(200) NOT NULL,
  estado varchar(20) NOT NULL DEFAULT 'PENDIENTE',
  tecnico_id uuid,
  creado_por uuid,
  notas_tecnico varchar(500),
  fecha_creacion timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_entrega timestamp without time zone,
  fecha_completado timestamp without time zone,
  mano_obra numeric(10,2) NOT NULL DEFAULT 0,
  total_orden numeric(10,2) NOT NULL DEFAULT 0,
  anticipo_porcentaje numeric(5,2) NOT NULL DEFAULT 0,
  anticipo_recibido numeric(10,2) NOT NULL DEFAULT 0,
  estado_pago varchar(20) NOT NULL DEFAULT 'PENDIENTE',
  updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT entregas_pkey PRIMARY KEY (id),
  CONSTRAINT entregas_tecnico_fkey FOREIGN KEY (tecnico_id)
    REFERENCES public.usuarios(id) ON DELETE SET NULL,
  CONSTRAINT entregas_tipo_fkey FOREIGN KEY (tipo)
    REFERENCES public.catalogo_tipos_entrega(codigo) ON DELETE RESTRICT,
  CONSTRAINT entregas_creado_por_fkey FOREIGN KEY (creado_por)
    REFERENCES public.usuarios(id) ON DELETE SET NULL,
  CONSTRAINT entregas_estado_fkey FOREIGN KEY (estado)
    REFERENCES public.catalogo_estados_orden(codigo) ON DELETE RESTRICT,
  CONSTRAINT entregas_estado_pago_fkey FOREIGN KEY (estado_pago)
    REFERENCES public.catalogo_estados_pago(codigo) ON DELETE RESTRICT,
  CONSTRAINT entregas_cliente_fkey FOREIGN KEY (cliente_id)
    REFERENCES public.clientes(id) ON DELETE SET NULL,
  CONSTRAINT entregas_total_orden_check CHECK (total_orden >= 0),
  CONSTRAINT entregas_anticipo_recibido_check CHECK (anticipo_recibido >= 0),
  CONSTRAINT entregas_mano_obra_check CHECK (mano_obra >= 0),
  CONSTRAINT entregas_anticipo_porcentaje_check CHECK (anticipo_porcentaje >= 0 AND anticipo_porcentaje <= 100)
);

CREATE TABLE public.ventas (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  cliente_nombre varchar(120) NOT NULL,
  cliente_telefono varchar(20),
  cliente_id uuid,
  vendedor_id uuid,
  estado varchar(20) NOT NULL DEFAULT 'COMPLETADA',
  total numeric(10,2) NOT NULL DEFAULT 0,
  fecha timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ventas_pkey PRIMARY KEY (id),
  CONSTRAINT ventas_cliente_fkey FOREIGN KEY (cliente_id)
    REFERENCES public.clientes(id) ON DELETE SET NULL,
  CONSTRAINT ventas_estado_fkey FOREIGN KEY (estado)
    REFERENCES public.catalogo_estados_venta(codigo) ON DELETE RESTRICT,
  CONSTRAINT ventas_vendedor_fkey FOREIGN KEY (vendedor_id)
    REFERENCES public.usuarios(id) ON DELETE SET NULL,
  CONSTRAINT ventas_total_check CHECK (total >= 0)
);

CREATE TABLE public.compra_items (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  compra_id uuid NOT NULL,
  producto_id uuid NOT NULL,
  cantidad integer NOT NULL,
  precio_unitario numeric(10,2) NOT NULL,
  CONSTRAINT compra_items_pkey PRIMARY KEY (id),
  CONSTRAINT compra_items_compra_fkey FOREIGN KEY (compra_id)
    REFERENCES public.compras(id) ON DELETE CASCADE,
  CONSTRAINT compra_items_producto_fkey FOREIGN KEY (producto_id)
    REFERENCES public.productos(id) ON DELETE RESTRICT,
  CONSTRAINT compra_items_cantidad_check CHECK (cantidad > 0),
  CONSTRAINT compra_items_precio_unitario_check CHECK (precio_unitario >= 0)
);

CREATE TABLE public.entrega_items (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  entrega_id uuid NOT NULL,
  producto_id uuid NOT NULL,
  cantidad integer NOT NULL DEFAULT 1,
  precio_unitario numeric(10,2) NOT NULL DEFAULT 0,
  CONSTRAINT entrega_items_pkey PRIMARY KEY (id),
  CONSTRAINT entrega_items_entrega_fk FOREIGN KEY (entrega_id)
    REFERENCES public.entregas(id) ON DELETE CASCADE,
  CONSTRAINT entrega_items_producto_fk FOREIGN KEY (producto_id)
    REFERENCES public.productos(id) ON DELETE RESTRICT,
  CONSTRAINT entrega_items_cantidad_check CHECK (cantidad >= 1),
  CONSTRAINT entrega_items_precio_unitario_check CHECK (precio_unitario >= 0)
);

CREATE TABLE public.pagos_orden (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  entrega_id uuid NOT NULL,
  usuario_id uuid,
  monto numeric(10,2) NOT NULL,
  notas varchar(255),
  fecha timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pagos_orden_pkey PRIMARY KEY (id),
  CONSTRAINT pagos_orden_entrega_fk FOREIGN KEY (entrega_id)
    REFERENCES public.entregas(id) ON DELETE CASCADE,
  CONSTRAINT pagos_orden_usuario_fk FOREIGN KEY (usuario_id)
    REFERENCES public.usuarios(id) ON DELETE SET NULL,
  CONSTRAINT pagos_orden_monto_check CHECK (monto > 0)
);

CREATE TABLE public.venta_items (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  venta_id uuid NOT NULL,
  producto_id uuid NOT NULL,
  cantidad integer NOT NULL,
  precio_unitario numeric(10,2) NOT NULL,
  CONSTRAINT venta_items_pkey PRIMARY KEY (id),
  CONSTRAINT venta_items_producto_fkey FOREIGN KEY (producto_id)
    REFERENCES public.productos(id) ON DELETE RESTRICT,
  CONSTRAINT venta_items_venta_fkey FOREIGN KEY (venta_id)
    REFERENCES public.ventas(id) ON DELETE CASCADE,
  CONSTRAINT venta_items_cantidad_check CHECK (cantidad > 0),
  CONSTRAINT venta_items_precio_unitario_check CHECK (precio_unitario >= 0)
);

CREATE TABLE public.facturas_orden (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  entrega_id uuid NOT NULL,
  numero varchar(20) NOT NULL DEFAULT ('FO-' || lpad(nextval('public.seq_facturas_orden'::regclass)::text, 4, '0')),
  subtotal numeric(10,2) NOT NULL DEFAULT 0,
  impuesto numeric(10,2) NOT NULL DEFAULT 0,
  total numeric(10,2) NOT NULL DEFAULT 0,
  notas text,
  fecha_emision timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT facturas_orden_pkey PRIMARY KEY (id),
  CONSTRAINT facturas_orden_entrega_id_key UNIQUE (entrega_id),
  CONSTRAINT facturas_orden_numero_key UNIQUE (numero),
  CONSTRAINT facturas_orden_entrega_fk FOREIGN KEY (entrega_id)
    REFERENCES public.entregas(id) ON DELETE CASCADE,
  CONSTRAINT facturas_orden_impuesto_check CHECK (impuesto >= 0),
  CONSTRAINT facturas_orden_subtotal_check CHECK (subtotal >= 0),
  CONSTRAINT facturas_orden_total_check CHECK (total >= 0)
);

CREATE TABLE public.facturas_venta (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  venta_id uuid NOT NULL,
  numero varchar(20) NOT NULL DEFAULT ('FV-' || lpad(nextval('public.seq_facturas_venta'::regclass)::text, 4, '0')),
  subtotal numeric(10,2) NOT NULL DEFAULT 0,
  impuesto numeric(10,2) NOT NULL DEFAULT 0,
  total numeric(10,2) NOT NULL DEFAULT 0,
  notas text,
  fecha_emision timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT facturas_venta_pkey PRIMARY KEY (id),
  CONSTRAINT facturas_venta_venta_id_key UNIQUE (venta_id),
  CONSTRAINT facturas_venta_numero_key UNIQUE (numero),
  CONSTRAINT facturas_venta_venta_fk FOREIGN KEY (venta_id)
    REFERENCES public.ventas(id) ON DELETE CASCADE,
  CONSTRAINT facturas_venta_subtotal_check CHECK (subtotal >= 0),
  CONSTRAINT facturas_venta_impuesto_check CHECK (impuesto >= 0),
  CONSTRAINT facturas_venta_total_check CHECK (total >= 0)
);

CREATE TABLE public.devoluciones_garantias (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  tipo varchar(20) NOT NULL DEFAULT 'DEVOLUCION',
  estado varchar(20) NOT NULL DEFAULT 'REGISTRADA',
  venta_id uuid,
  entrega_id uuid,
  cliente_id uuid,
  cliente_nombre varchar(120),
  razon text NOT NULL,
  accion_dinero varchar(20) NOT NULL DEFAULT 'REEMBOLSO',
  monto_devuelto numeric(10,2) NOT NULL DEFAULT 0,
  notas text,
  registrado_por uuid,
  fecha timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT devoluciones_garantias_pkey PRIMARY KEY (id),
  CONSTRAINT devoluciones_garantias_cliente_fkey FOREIGN KEY (cliente_id)
    REFERENCES public.clientes(id) ON DELETE SET NULL,
  CONSTRAINT devoluciones_garantias_entrega_fkey FOREIGN KEY (entrega_id)
    REFERENCES public.entregas(id) ON DELETE RESTRICT,
  CONSTRAINT devoluciones_garantias_estado_fkey FOREIGN KEY (estado)
    REFERENCES public.catalogo_estados_devolucion(codigo) ON DELETE RESTRICT,
  CONSTRAINT devoluciones_garantias_accion_dinero_fkey FOREIGN KEY (accion_dinero)
    REFERENCES public.catalogo_acciones_dinero_devolucion(codigo) ON DELETE RESTRICT,
  CONSTRAINT devoluciones_garantias_usuario_fkey FOREIGN KEY (registrado_por)
    REFERENCES public.usuarios(id) ON DELETE SET NULL,
  CONSTRAINT devoluciones_garantias_venta_fkey FOREIGN KEY (venta_id)
    REFERENCES public.ventas(id) ON DELETE RESTRICT,
  CONSTRAINT devoluciones_garantias_tipo_fkey FOREIGN KEY (tipo)
    REFERENCES public.catalogo_tipos_devolucion(codigo) ON DELETE RESTRICT,
  CONSTRAINT devoluciones_garantias_origen_check CHECK (
    (venta_id IS NOT NULL AND entrega_id IS NULL) OR
    (venta_id IS NULL AND entrega_id IS NOT NULL)
  ),
  CONSTRAINT devoluciones_garantias_monto_check CHECK (monto_devuelto >= 0)
);

CREATE TABLE public.devolucion_garantia_items (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  devolucion_id uuid NOT NULL,
  producto_id uuid NOT NULL,
  cantidad integer NOT NULL,
  precio_unitario numeric(10,2) NOT NULL DEFAULT 0,
  motivo_item varchar(255),
  CONSTRAINT devolucion_garantia_items_pkey PRIMARY KEY (id),
  CONSTRAINT devolucion_garantia_items_devolucion_fkey FOREIGN KEY (devolucion_id)
    REFERENCES public.devoluciones_garantias(id) ON DELETE CASCADE,
  CONSTRAINT devolucion_garantia_items_producto_fkey FOREIGN KEY (producto_id)
    REFERENCES public.productos(id) ON DELETE RESTRICT,
  CONSTRAINT devolucion_garantia_items_cantidad_check CHECK (cantidad > 0),
  CONSTRAINT devolucion_garantia_items_precio_check CHECK (precio_unitario >= 0)
);

CREATE TABLE public.movimientos_caja (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  caja_id uuid NOT NULL,
  tipo varchar(20) NOT NULL,
  concepto varchar(255) NOT NULL,
  monto numeric(10,2) NOT NULL,
  referencia varchar(100),
  usuario_id uuid,
  fecha timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  notas text,
  created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT movimientos_caja_pkey PRIMARY KEY (id),
  CONSTRAINT movimientos_caja_caja_fkey FOREIGN KEY (caja_id)
    REFERENCES public.cajas(id) ON DELETE CASCADE,
  CONSTRAINT movimientos_caja_tipo_fkey FOREIGN KEY (tipo)
    REFERENCES public.catalogo_tipos_movimiento_caja(codigo) ON DELETE RESTRICT,
  CONSTRAINT movimientos_caja_usuario_fkey FOREIGN KEY (usuario_id)
    REFERENCES public.usuarios(id) ON DELETE SET NULL,
  CONSTRAINT movimientos_caja_monto_check CHECK (monto > 0)
);

CREATE TABLE public.movimientos_inventario (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  producto_id uuid NOT NULL,
  tipo varchar(10) NOT NULL,
  cantidad integer NOT NULL,
  proveedor_id uuid,
  usuario_id uuid,
  observacion varchar(255),
  origen_tipo varchar(20),
  origen_id uuid,
  fecha timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT movimientos_inventario_pkey PRIMARY KEY (id),
  CONSTRAINT mov_producto_fkey FOREIGN KEY (producto_id)
    REFERENCES public.productos(id) ON DELETE RESTRICT,
  CONSTRAINT mov_proveedor_fkey FOREIGN KEY (proveedor_id)
    REFERENCES public.proveedores(id) ON DELETE SET NULL,
  CONSTRAINT mov_usuario_fkey FOREIGN KEY (usuario_id)
    REFERENCES public.usuarios(id) ON DELETE SET NULL,
  CONSTRAINT mov_tipo_fkey FOREIGN KEY (tipo)
    REFERENCES public.catalogo_tipos_movimiento(codigo) ON DELETE RESTRICT,
  CONSTRAINT movimientos_inventario_cantidad_check CHECK (cantidad > 0),
  CONSTRAINT mov_origen_tipo_check CHECK (
    origen_tipo IS NULL OR origen_tipo IN ('VENTA', 'COMPRA', 'ENTREGA', 'DEVOLUCION', 'AJUSTE', 'MANUAL')
  )
);

-- Datos de referencia documentados. No se crean usuarios ni se copian datos privados.
INSERT INTO public.catalogo_acciones_dinero_devolucion (codigo, descripcion) VALUES
  ('REEMBOLSO', 'Se devuelve dinero al cliente'),
  ('SALDO_FAVOR', 'Se deja saldo a favor del cliente'),
  ('SIN_REEMBOLSO', 'No hay movimiento de dinero');

INSERT INTO public.catalogo_estados_caja (codigo, descripcion, orden_visual) VALUES
  ('ABIERTA', 'Caja abierta, recibiendo transacciones', 1),
  ('CERRADA', 'Caja cerrada y contabilizada', 2),
  ('ANULADA', 'Caja anulada sin aplicar movimientos', 3);

INSERT INTO public.catalogo_estados_compra (codigo, descripcion, orden_visual) VALUES
  ('PENDIENTE', 'Orden creada, no confirmada con el proveedor', 1),
  ('CONFIRMADA', 'Proveedor confirmó; productos en camino', 2),
  ('CANCELADA', 'Compra cancelada', 3);

INSERT INTO public.catalogo_estados_devolucion (codigo, descripcion, orden_visual) VALUES
  ('REGISTRADA', 'Registro aplicado al inventario y trazabilidad', 1),
  ('ANULADA', 'Registro reversado con movimiento contrario', 2);

INSERT INTO public.catalogo_estados_orden (codigo, descripcion, orden_visual) VALUES
  ('PENDIENTE', 'Orden creada, sin técnico asignado', 1),
  ('EN_PROCESO', 'Técnico asignado, trabajo en curso', 2),
  ('COMPLETADO', 'Trabajo finalizado, pendiente de pago', 3),
  ('CANCELADO', 'Orden cancelada', 4),
  ('FINALIZADO', 'Cerrado por admin — inventario aplicado', 5);

INSERT INTO public.catalogo_estados_pago (codigo, descripcion, orden_visual) VALUES
  ('PENDIENTE', 'Sin pagos registrados', 1),
  ('ANTICIPO', 'Se recibió el anticipo acordado', 2),
  ('PARCIAL', 'Pago parcial recibido, sin completar', 3),
  ('COMPLETO', 'Orden pagada en su totalidad', 4);

INSERT INTO public.catalogo_estados_venta (codigo, descripcion, orden_visual) VALUES
  ('PENDIENTE', 'Venta registrada, pago pendiente', 1),
  ('COMPLETADA', 'Venta finalizada y cobrada', 2),
  ('CANCELADA', 'Venta anulada', 3);

INSERT INTO public.catalogo_tipos_devolucion (codigo, descripcion) VALUES
  ('DEVOLUCION', 'Devolucion de producto facturado'),
  ('GARANTIA', 'Atencion por garantia sin reembolso');

INSERT INTO public.catalogo_tipos_entrega (codigo, descripcion) VALUES
  ('REPARACION', 'Reparación de equipo o mobiliario'),
  ('INSTALACION', 'Instalación en sitio del cliente'),
  ('ENTREGA', 'Despacho de producto al cliente');

INSERT INTO public.catalogo_tipos_movimiento (codigo, descripcion) VALUES
  ('ENTRADA', 'Incrementa el stock del producto'),
  ('SALIDA', 'Decrementa el stock del producto');

INSERT INTO public.catalogo_tipos_movimiento_caja (codigo, descripcion, afecta_saldo, orden_visual) VALUES
  ('INGRESO', 'Ingreso manual de efectivo', 'INGRESO', 1),
  ('PAGO_VENTA', 'Pago recibido por venta directa', 'INGRESO', 2),
  ('PAGO_ORDEN', 'Pago recibido por orden servicio', 'INGRESO', 3),
  ('EGRESO', 'Egreso manual de efectivo', 'EGRESO', 4),
  ('COMPRA', 'Pago o salida por compra', 'EGRESO', 5),
  ('AJUSTE', 'Ajuste operativo de caja', 'NEUTRO', 6),
  ('DEVOLUCION', 'Salida por devolucion o garantia', 'EGRESO', 7);

INSERT INTO public.categorias (id, nombre, descripcion, activo) VALUES
  ('06359856-9dc9-455c-a1e3-e71b5be19e41', 'Escritorio', 'Escritorios y superficies de trabajo', true),
  ('1669da6e-4308-40a3-87a6-feefb89cf195', 'Herramienta', 'Herramientas de uso técnico', true),
  ('66382854-e693-4182-9d9b-0021a7ec2d7a', 'Araña', 'Bases tipo araña para sillas', true),
  ('92797d77-4ef0-4c5d-ac1f-8daa5bc34ef5', 'Silla oficina', 'Sillas y asientos ergonómicos para oficina', true),
  ('93aac1fd-5e9d-47c3-8467-5c9c5370080e', 'Guaya', 'Cables y guayas para mecanismos', true),
  ('c0acd70d-ee3e-446d-9a35-01d2f7755cde', 'Rodachín', 'Ruedas y rodachines para sillas', true),
  ('d66b3b78-8dab-493f-b592-2d54b1f0d78a', 'Cilindro', 'Cilindros neumáticos para sillas', true),
  ('efffd905-970a-45a4-9af0-c07b574494d0', 'Insumo general', 'Materiales de uso general', true);

INSERT INTO public.roles (nombre, activo) VALUES
  ('ADMIN', true),
  ('VENDEDOR', true),
  ('TECNICO', true),
  ('INVENTARIO', true);

-- Índices del esquema extraído.
CREATE INDEX idx_auditoria_usuario ON public.auditoria USING btree (usuario_id);
CREATE INDEX idx_auditoria_modulo ON public.auditoria USING btree (modulo);
CREATE INDEX idx_auditoria_accion ON public.auditoria USING btree (accion);
CREATE INDEX idx_auditoria_fecha ON public.auditoria USING btree (fecha DESC);
CREATE INDEX idx_auditoria_usuario_fecha ON public.auditoria USING btree (usuario_id, fecha DESC);
CREATE INDEX idx_auditoria_entidad ON public.auditoria USING btree (entidad_tipo, entidad_id);
CREATE INDEX idx_auditoria_resultado_fecha ON public.auditoria USING btree (resultado, fecha DESC);
CREATE INDEX idx_auditoria_severidad_fecha ON public.auditoria USING btree (severidad, fecha DESC);
CREATE INDEX idx_auditoria_ruta_fecha ON public.auditoria USING btree (ruta, fecha DESC);
CREATE INDEX idx_auditoria_metadata_gin ON public.auditoria USING gin (metadata);
CREATE INDEX idx_cajas_usuario_id ON public.cajas USING btree (usuario_id);
CREATE INDEX idx_cajas_estado ON public.cajas USING btree (estado);
CREATE INDEX idx_cajas_fecha_apertura ON public.cajas USING btree (fecha_apertura DESC);
CREATE INDEX idx_categorias_activo ON public.categorias USING btree (activo);
CREATE INDEX idx_clientes_nombre ON public.clientes USING btree (nombre);
CREATE INDEX idx_clientes_activo ON public.clientes USING btree (activo);
CREATE INDEX idx_compra_items_compra ON public.compra_items USING btree (compra_id);
CREATE INDEX idx_compra_items_producto ON public.compra_items USING btree (producto_id);
CREATE INDEX idx_compras_proveedor ON public.compras USING btree (proveedor_id);
CREATE INDEX idx_compras_estado ON public.compras USING btree (estado);
CREATE INDEX idx_compras_fecha ON public.compras USING btree (fecha DESC);
CREATE INDEX idx_devolucion_items_devolucion ON public.devolucion_garantia_items USING btree (devolucion_id);
CREATE INDEX idx_devolucion_items_producto ON public.devolucion_garantia_items USING btree (producto_id);
CREATE INDEX idx_devoluciones_venta ON public.devoluciones_garantias USING btree (venta_id);
CREATE INDEX idx_devoluciones_entrega ON public.devoluciones_garantias USING btree (entrega_id);
CREATE INDEX idx_devoluciones_fecha ON public.devoluciones_garantias USING btree (fecha DESC);
CREATE INDEX idx_devoluciones_cliente ON public.devoluciones_garantias USING btree (cliente_id);
CREATE INDEX idx_entrega_items_entrega ON public.entrega_items USING btree (entrega_id);
CREATE INDEX idx_entrega_items_producto ON public.entrega_items USING btree (producto_id);
CREATE INDEX idx_entregas_cliente ON public.entregas USING btree (cliente_id);
CREATE INDEX idx_entregas_tecnico ON public.entregas USING btree (tecnico_id);
CREATE INDEX idx_entregas_estado ON public.entregas USING btree (estado);
CREATE INDEX idx_entregas_estado_pago ON public.entregas USING btree (estado_pago);
CREATE INDEX idx_entregas_tipo ON public.entregas USING btree (tipo);
CREATE INDEX idx_entregas_fecha ON public.entregas USING btree (fecha_creacion DESC);
CREATE INDEX idx_facturas_orden_entrega ON public.facturas_orden USING btree (entrega_id);
CREATE INDEX idx_facturas_venta_venta ON public.facturas_venta USING btree (venta_id);
CREATE INDEX idx_movimientos_caja_caja_id ON public.movimientos_caja USING btree (caja_id);
CREATE INDEX idx_movimientos_caja_tipo ON public.movimientos_caja USING btree (tipo);
CREATE INDEX idx_movimientos_caja_fecha ON public.movimientos_caja USING btree (fecha DESC);
CREATE INDEX idx_movimientos_caja_usuario_id ON public.movimientos_caja USING btree (usuario_id);
CREATE INDEX idx_mov_origen ON public.movimientos_inventario USING btree (origen_tipo, origen_id);
CREATE INDEX idx_mov_producto ON public.movimientos_inventario USING btree (producto_id);
CREATE INDEX idx_mov_proveedor ON public.movimientos_inventario USING btree (proveedor_id);
CREATE INDEX idx_mov_fecha ON public.movimientos_inventario USING btree (fecha DESC);
CREATE INDEX idx_pagos_orden_entrega ON public.pagos_orden USING btree (entrega_id);
CREATE INDEX idx_pagos_orden_fecha ON public.pagos_orden USING btree (fecha DESC);
CREATE INDEX idx_productos_codigo ON public.productos USING btree (codigo);
CREATE INDEX idx_productos_categoria ON public.productos USING btree (categoria_id);
CREATE INDEX idx_productos_activo ON public.productos USING btree (activo);
CREATE INDEX idx_proveedores_nombre ON public.proveedores USING btree (nombre);
CREATE INDEX idx_proveedores_activo ON public.proveedores USING btree (activo);
CREATE INDEX idx_usuarios_rol ON public.usuarios USING btree (rol_id);
CREATE INDEX idx_usuarios_activo ON public.usuarios USING btree (activo);
CREATE INDEX idx_venta_items_venta ON public.venta_items USING btree (venta_id);
CREATE INDEX idx_venta_items_producto ON public.venta_items USING btree (producto_id);
CREATE INDEX idx_ventas_cliente ON public.ventas USING btree (cliente_id);
CREATE INDEX idx_ventas_vendedor ON public.ventas USING btree (vendedor_id);
CREATE INDEX idx_ventas_estado ON public.ventas USING btree (estado);
CREATE INDEX idx_ventas_fecha ON public.ventas USING btree (fecha DESC);

-- Triggers: se instalan después de todas las tablas.
CREATE TRIGGER trg_auditoria_no_update_delete
  BEFORE DELETE OR UPDATE ON public.auditoria
  FOR EACH ROW EXECUTE FUNCTION public.fn_auditoria_bloquear_modificaciones();
CREATE TRIGGER trg_cajas_updated_at
  BEFORE UPDATE ON public.cajas
  FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();
CREATE TRIGGER trg_clientes_updated_at
  BEFORE UPDATE ON public.clientes
  FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();
CREATE TRIGGER trg_compras_updated_at
  BEFORE UPDATE ON public.compras
  FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();
CREATE TRIGGER trg_devoluciones_garantias_updated_at
  BEFORE UPDATE ON public.devoluciones_garantias
  FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();
CREATE TRIGGER trg_entregas_updated_at
  BEFORE UPDATE ON public.entregas
  FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();
CREATE TRIGGER trg_actualizar_stock
  AFTER INSERT ON public.movimientos_inventario
  FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_stock();
CREATE TRIGGER trg_productos_updated_at
  BEFORE UPDATE ON public.productos
  FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();
CREATE TRIGGER trg_proveedores_updated_at
  BEFORE UPDATE ON public.proveedores
  FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();
CREATE TRIGGER trg_usuarios_updated_at
  BEFORE UPDATE ON public.usuarios
  FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();
CREATE TRIGGER trg_ventas_updated_at
  BEFORE UPDATE ON public.ventas
  FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();

-- Vistas documentadas para analítica.
CREATE VIEW public.vw_ventas_por_vendedor AS
SELECT v.vendedor_id, u.nombre AS vendedor_nombre, u.email,
       COUNT(v.id) AS total_ventas,
       SUM(CASE WHEN v.estado = 'COMPLETADA' THEN 1 ELSE 0 END)::integer AS ventas_completadas,
       COALESCE(SUM(CASE WHEN v.estado = 'COMPLETADA' THEN v.total ELSE 0 END), 0::numeric(10,2)) AS total_ingreso,
       MAX(v.fecha) AS ultima_venta, MIN(v.fecha) AS primera_venta
FROM public.ventas v
LEFT JOIN public.usuarios u ON v.vendedor_id = u.id
WHERE v.vendedor_id IS NOT NULL
GROUP BY v.vendedor_id, u.nombre, u.email
ORDER BY total_ingreso DESC NULLS LAST;

CREATE VIEW public.vw_rentabilidad_categoria AS
SELECT c.id, c.nombre AS categoria,
       COUNT(DISTINCT p.id) AS productos_activos,
       COUNT(DISTINCT CASE WHEN v.id IS NOT NULL THEN vi.id END) AS items_vendidos,
       COALESCE(SUM(CASE WHEN v.id IS NOT NULL THEN vi.cantidad ELSE 0 END), 0)::integer AS cantidad_total_vendida,
       COALESCE(SUM(CASE WHEN v.id IS NOT NULL THEN vi.cantidad * vi.precio_unitario ELSE 0 END), 0::numeric(10,2))::numeric(10,2) AS ingreso_total,
       COALESCE(ROUND(
         SUM(CASE WHEN v.id IS NOT NULL THEN vi.cantidad * vi.precio_unitario ELSE 0 END)
         / NULLIF(SUM(CASE WHEN v.id IS NOT NULL THEN vi.cantidad ELSE 0 END), 0), 2
       ), 0::numeric(10,2)) AS precio_promedio,
       (COALESCE(SUM(CASE WHEN v.id IS NOT NULL THEN vi.cantidad * vi.precio_unitario ELSE 0 END), 0::numeric(10,2)) * 0.6)::numeric(10,2) AS costo_estimado,
       (COALESCE(SUM(CASE WHEN v.id IS NOT NULL THEN vi.cantidad * vi.precio_unitario ELSE 0 END), 0::numeric(10,2)) * 0.4)::numeric(10,2) AS ganancia_estimada,
       40.0::numeric(5,2) AS margen_porcentaje
FROM public.categorias c
LEFT JOIN public.productos p ON c.id = p.categoria_id AND p.activo = true
LEFT JOIN public.venta_items vi ON p.id = vi.producto_id
LEFT JOIN public.ventas v ON vi.venta_id = v.id AND v.estado = 'COMPLETADA'
GROUP BY c.id, c.nombre
ORDER BY ingreso_total DESC NULLS LAST;

CREATE VIEW public.vw_rendimiento_tecnicos AS
SELECT e.tecnico_id, u.nombre AS tecnico_nombre, u.email,
       COUNT(e.id) AS ordenes_totales,
       SUM(CASE WHEN e.estado = 'FINALIZADO' THEN 1 ELSE 0 END)::integer AS ordenes_completadas,
       SUM(CASE WHEN e.estado = 'FINALIZADO' AND e.fecha_entrega IS NOT NULL
                 AND (e.fecha_entrega - e.fecha_creacion) <= INTERVAL '1 day'
                THEN 1 ELSE 0 END)::integer AS ordenes_24horas,
       COALESCE(ROUND(AVG(CASE WHEN e.fecha_entrega IS NOT NULL
         THEN EXTRACT(EPOCH FROM (e.fecha_entrega - e.fecha_creacion)) / 86400 END)::numeric, 2), 0::numeric) AS dias_promedio,
       COALESCE(SUM(CASE WHEN e.estado = 'FINALIZADO' THEN e.mano_obra ELSE 0 END), 0::numeric(10,2)) AS total_mano_obra,
       COALESCE(SUM(CASE WHEN e.estado = 'FINALIZADO' THEN e.total_orden ELSE 0 END), 0::numeric(10,2)) AS ingreso_total,
       SUM(CASE WHEN e.estado_pago IN ('PENDIENTE', 'ANTICIPO', 'PARCIAL') THEN 1 ELSE 0 END)::integer AS ordenes_deuda,
       MAX(e.fecha_creacion) AS ultima_orden
FROM public.entregas e
LEFT JOIN public.usuarios u ON e.tecnico_id = u.id
WHERE e.tecnico_id IS NOT NULL
GROUP BY e.tecnico_id, u.nombre, u.email
ORDER BY ordenes_completadas DESC NULLS LAST;

CREATE VIEW public.vw_flujo_caja_30dias AS
SELECT DATE(mc.fecha) AS fecha, mc.tipo,
       COUNT(*) AS cantidad_movimientos,
       SUM(mc.monto)::numeric(10,2) AS monto_total,
       SUM(CASE WHEN tmc.afecta_saldo = 'INGRESO' THEN mc.monto ELSE 0 END)::numeric(10,2) AS ingresos,
       SUM(CASE WHEN tmc.afecta_saldo = 'EGRESO' THEN mc.monto ELSE 0 END)::numeric(10,2) AS egresos
FROM public.movimientos_caja mc
JOIN public.catalogo_tipos_movimiento_caja tmc ON mc.tipo = tmc.codigo
WHERE mc.fecha >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(mc.fecha), mc.tipo
ORDER BY fecha DESC;

-- Flujo histórico y proyección de caja.
-- El histórico devuelve el saldo de cierre diario reconstruido para las cajas
-- que siguen abiertas hoy. El pronóstico parte del saldo actual de esas cajas.
-- Los promedios incluyen los 30 días calendario (incluidos los días sin
-- movimientos), para evitar promediar únicamente los días activos.
CREATE OR REPLACE FUNCTION public.fn_flujo_caja_proyectado(dias_adelante integer DEFAULT 30)
RETURNS TABLE (
  fecha date,
  tipo_flujo character varying,
  ingresos numeric,
  egresos numeric,
  saldo_neto numeric,
  saldo_acumulado numeric
)
LANGUAGE plpgsql
AS $$
DECLARE
  v_saldo_actual numeric;
  v_fecha_inicio date;
  v_promedio_ingresos_diarios numeric;
  v_promedio_egresos_diarios numeric;
BEGIN
  v_fecha_inicio := CURRENT_DATE - 29;

  IF dias_adelante IS NOT NULL AND dias_adelante < 0 THEN
    RAISE EXCEPTION 'dias_adelante no puede ser negativo';
  END IF;

  -- Sumar cada apertura una sola vez; agregar los movimientos por caja antes
  -- de unirlos evita multiplicar monto_inicial por la cantidad de movimientos.
  SELECT COALESCE(SUM(c.monto_inicial + COALESCE(m.saldo_movimientos, 0)), 0)
    INTO v_saldo_actual
    FROM public.cajas c
    LEFT JOIN (
      SELECT mc.caja_id,
             SUM(CASE WHEN tmc.afecta_saldo = 'INGRESO' THEN mc.monto
                      WHEN tmc.afecta_saldo = 'EGRESO' THEN -mc.monto
                      ELSE 0 END) AS saldo_movimientos
        FROM public.movimientos_caja mc
        JOIN public.catalogo_tipos_movimiento_caja tmc ON tmc.codigo = mc.tipo
       GROUP BY mc.caja_id
    ) m ON m.caja_id = c.id
   WHERE c.estado = 'ABIERTA';

  -- Promedio diario de los últimos 30 días calendario, incluidos ceros.
  SELECT COALESCE(AVG(d.ingresos), 0), COALESCE(AVG(d.egresos), 0)
    INTO v_promedio_ingresos_diarios, v_promedio_egresos_diarios
    FROM generate_series(v_fecha_inicio, CURRENT_DATE, INTERVAL '1 day') AS cal(dia)
    CROSS JOIN LATERAL (
      SELECT COALESCE(SUM(CASE WHEN tmc.afecta_saldo = 'INGRESO' THEN mc.monto ELSE 0 END), 0) AS ingresos,
             COALESCE(SUM(CASE WHEN tmc.afecta_saldo = 'EGRESO' THEN mc.monto ELSE 0 END), 0) AS egresos
        FROM public.movimientos_caja mc
        JOIN public.cajas c ON c.id = mc.caja_id AND c.estado = 'ABIERTA'
        JOIN public.catalogo_tipos_movimiento_caja tmc ON tmc.codigo = mc.tipo
       WHERE mc.fecha >= cal.dia
         AND mc.fecha < cal.dia + INTERVAL '1 day'
    ) d;

  -- Una fila por día del período. El saldo se reconstruye por caja con su
  -- apertura y movimientos hasta el cierre de ese día; no usa movimientos
  -- posteriores ni duplica el monto inicial.
  RETURN QUERY
  WITH dias AS (
    SELECT gs::date AS fecha
      FROM generate_series(v_fecha_inicio, CURRENT_DATE, INTERVAL '1 day') AS gs
  ), flujo AS (
    SELECT d.fecha,
           COALESCE(SUM(CASE WHEN tmc.afecta_saldo = 'INGRESO' THEN mc.monto ELSE 0 END), 0) AS ingresos,
           COALESCE(SUM(CASE WHEN tmc.afecta_saldo = 'EGRESO' THEN mc.monto ELSE 0 END), 0) AS egresos
      FROM dias d
      LEFT JOIN public.movimientos_caja mc
        ON mc.fecha >= d.fecha AND mc.fecha < d.fecha + INTERVAL '1 day'
      LEFT JOIN public.cajas c ON c.id = mc.caja_id AND c.estado = 'ABIERTA'
      LEFT JOIN public.catalogo_tipos_movimiento_caja tmc ON tmc.codigo = mc.tipo
     WHERE mc.id IS NULL OR c.id IS NOT NULL
     GROUP BY d.fecha
  ), saldos AS (
    SELECT d.fecha,
           COALESCE(SUM(c.monto_inicial + COALESCE(m.saldo_hasta_fecha, 0)), 0) AS saldo
      FROM dias d
      LEFT JOIN public.cajas c
        ON c.estado = 'ABIERTA' AND c.fecha_apertura < d.fecha + INTERVAL '1 day'
      LEFT JOIN LATERAL (
        SELECT SUM(CASE WHEN tmc.afecta_saldo = 'INGRESO' THEN mc.monto
                        WHEN tmc.afecta_saldo = 'EGRESO' THEN -mc.monto
                        ELSE 0 END) AS saldo_hasta_fecha
          FROM public.movimientos_caja mc
          JOIN public.catalogo_tipos_movimiento_caja tmc ON tmc.codigo = mc.tipo
         WHERE mc.caja_id = c.id
           AND mc.fecha >= c.fecha_apertura
           AND mc.fecha < d.fecha + INTERVAL '1 day'
      ) m ON true
     GROUP BY d.fecha
  )
  SELECT f.fecha, 'HISTORICO'::varchar,
         f.ingresos::numeric(10,2), f.egresos::numeric(10,2),
         (f.ingresos - f.egresos)::numeric(10,2), s.saldo::numeric(10,2)
    FROM flujo f
    JOIN saldos s USING (fecha)
  UNION ALL
  SELECT (CURRENT_DATE + gs.n)::date, 'PROYECTADO'::varchar,
         v_promedio_ingresos_diarios::numeric(10,2),
         v_promedio_egresos_diarios::numeric(10,2),
         (v_promedio_ingresos_diarios - v_promedio_egresos_diarios)::numeric(10,2),
         (v_saldo_actual + gs.n * (v_promedio_ingresos_diarios - v_promedio_egresos_diarios))::numeric(10,2)
    FROM generate_series(1, COALESCE(dias_adelante, 30)) AS gs(n)
   ORDER BY fecha ASC;
END;
$$;
