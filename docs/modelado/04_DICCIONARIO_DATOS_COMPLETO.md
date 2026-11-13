# Diccionario de datos completo de ParkFlow

Extracción del DDL en `parkflow-db-definitivo`. Incluye columnas declaradas en V1 y las columnas añadidas mediante ALTER en migraciones posteriores. `—` en FK indica que no hay FK local; los UUID de otros servicios son referencias externas. Las descripciones son síntesis semánticas a partir del nombre y el esquema, no reglas adicionales.

## db_parking

| Tabla | Campo | Tipo de dato | PK | FK | Nulo | Default | Descripción |
|---|---|---|---|---|---|---|---|
| anulaciones_sesion | administrador_id | UUID | No | — | No | — | Identificador de referencia administrador id; confirmar FK en columna FK. |
| anulaciones_sesion | clave_idempotencia | UUID | No | — | No | — | Campo clave idempotencia de anulaciones_sesion. |
| anulaciones_sesion | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| anulaciones_sesion | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| anulaciones_sesion | motivo | TEXT | No | — | No | — | Campo motivo de anulaciones_sesion. |
| anulaciones_sesion | sesion_id | UUID | No | sesiones_parqueo.id | No | — | Identificador de referencia sesion id; confirmar FK en columna FK. |
| casos_abandono | actualizado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a actualizado en. |
| casos_abandono | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| casos_abandono | estado | VARCHAR(30) | No | — | No | 'ABANDONO_REPORTADO' | Campo estado de casos_abandono. |
| casos_abandono | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| casos_abandono | motivo | TEXT | No | — | No | — | Campo motivo de casos_abandono. |
| casos_abandono | operacion_cierre_id | UUID | No | — | Sí | — | Identificador de referencia operacion cierre id; confirmar FK en columna FK. |
| casos_abandono | operador_reporta_id | UUID | No | — | No | — | Identificador de referencia operador reporta id; confirmar FK en columna FK. |
| casos_abandono | reportado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a reportado en. |
| casos_abandono | sesion_id | UUID | No | sesiones_parqueo.id | No | — | Identificador de referencia sesion id; confirmar FK en columna FK. |
| contactos_autoridad | caso_abandono_id | UUID | No | casos_abandono.id | No | — | Identificador de referencia caso abandono id; confirmar FK en columna FK. |
| contactos_autoridad | contactado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a contactado en. |
| contactos_autoridad | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| contactos_autoridad | institucion | VARCHAR(100) | No | — | No | — | Campo institucion de contactos_autoridad. |
| contactos_autoridad | operador_id | UUID | No | — | No | — | Identificador de referencia operador id; confirmar FK en columna FK. |
| contactos_autoridad | resultado | TEXT | No | — | Sí | — | Campo resultado de contactos_autoridad. |
| espacios | actualizado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a actualizado en. |
| espacios | codigo | VARCHAR(20) | No | — | No | — | Campo codigo de espacios. |
| espacios | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| espacios | estado | VARCHAR(20) | No | — | No | 'HABILITADO' | Campo estado de espacios. |
| espacios | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| espacios | tipo_vehiculo | VARCHAR(10) | No | — | No | — | Campo tipo vehiculo de espacios. |
| espacios | zona_id | UUID | No | zonas.id | No | — | Identificador de referencia zona id; confirmar FK en columna FK. |
| eventos_operativos | clave_idempotencia | UUID | No | — | Sí | — | Campo clave idempotencia de eventos_operativos. |
| eventos_operativos | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| eventos_operativos | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| eventos_operativos | operador_id | UUID | No | — | No | — | Identificador de referencia operador id; confirmar FK en columna FK. |
| eventos_operativos | sesion_id | UUID | No | sesiones_parqueo.id | Sí | — | Identificador de referencia sesion id; confirmar FK en columna FK. |
| eventos_operativos | tipo_evento | VARCHAR(40) | No | — | No | — | Campo tipo evento de eventos_operativos. |
| evidencias_abandono | caso_abandono_id | UUID | No | casos_abandono.id | No | — | Identificador de referencia caso abandono id; confirmar FK en columna FK. |
| evidencias_abandono | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| evidencias_abandono | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| evidencias_abandono | registrado_por | UUID | No | — | No | — | Campo registrado por de evidencias_abandono. |
| evidencias_abandono | tipo | VARCHAR(30) | No | — | No | — | Campo tipo de evidencias_abandono. |
| mantenimientos_espacio | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| mantenimientos_espacio | descripcion | TEXT | No | — | No | — | Campo descripcion de mantenimientos_espacio. |
| mantenimientos_espacio | espacio_id | UUID | No | espacios.id | No | — | Identificador de referencia espacio id; confirmar FK en columna FK. |
| mantenimientos_espacio | estado | VARCHAR(15) | No | — | No | 'EN_PROCESO' | Campo estado de mantenimientos_espacio. |
| mantenimientos_espacio | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| mantenimientos_espacio | iniciado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a iniciado en. |
| mantenimientos_espacio | motivo | VARCHAR(20) | No | — | No | — | Campo motivo de mantenimientos_espacio. |
| mantenimientos_espacio | operador_responsable_id | UUID | No | — | No | — | Identificador de referencia operador responsable id; confirmar FK en columna FK. |
| pisos | activo | BOOLEAN | No | — | No | TRUE | Campo activo de pisos. |
| pisos | actualizado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a actualizado en. |
| pisos | codigo | VARCHAR(15) | No | — | No | — | Campo codigo de pisos. |
| pisos | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| pisos | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| pisos | nombre | VARCHAR(80) | No | — | No | — | Campo nombre de pisos. |
| pisos | numero | SMALLINT | No | — | No | — | Campo numero de pisos. |
| retiros_vehiculo | caso_abandono_id | UUID | No | casos_abandono.id | No | — | Identificador de referencia caso abandono id; confirmar FK en columna FK. |
| retiros_vehiculo | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| retiros_vehiculo | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| retiros_vehiculo | operador_entrega_id | UUID | No | — | No | — | Identificador de referencia operador entrega id; confirmar FK en columna FK. |
| retiros_vehiculo | responsable_retiro | VARCHAR(150) | No | — | No | — | Campo responsable retiro de retiros_vehiculo. |
| retiros_vehiculo | retirado_en | TIMESTAMPTZ | No | — | No | — | Fecha/hora asociada a retirado en. |
| retiros_vehiculo | tipo_responsable | VARCHAR(30) | No | — | No | — | Campo tipo responsable de retiros_vehiculo. |
| salidas_excepcionales | administrador_autorizador_id | UUID | No | — | No | — | Identificador de referencia administrador autorizador id; confirmar FK en columna FK. |
| salidas_excepcionales | autorizacion_identity_id | UUID | No | — | No | — | Identificador de referencia autorizacion identity id; confirmar FK en columna FK. |
| salidas_excepcionales | ejecutada_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a ejecutada en. |
| salidas_excepcionales | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| salidas_excepcionales | jti | UUID | No | — | No | — | Campo jti de salidas_excepcionales. |
| salidas_excepcionales | motivo | TEXT | No | — | No | — | Campo motivo de salidas_excepcionales. |
| salidas_excepcionales | operacion_id | UUID | No | — | No | — | Identificador de referencia operacion id; confirmar FK en columna FK. |
| salidas_excepcionales | operador_ejecutor_id | UUID | No | — | No | — | Identificador de referencia operador ejecutor id; confirmar FK en columna FK. |
| salidas_excepcionales | sesion_id | UUID | No | sesiones_parqueo.id | No | — | Identificador de referencia sesion id; confirmar FK en columna FK. |
| salidas_excepcionales | solicitud_id | UUID | No | solicitudes_salida_excepcional.id | No | — | Identificador de referencia solicitud id; confirmar FK en columna FK. |
| sesiones_parqueo | actualizado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a actualizado en. |
| sesiones_parqueo | clave_idempotencia | UUID | No | — | Sí | — | Clave de idempotencia de la entrada, agregada por V4. |
| sesiones_parqueo | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| sesiones_parqueo | espacio_id | UUID | No | espacios.id | No | — | Identificador de referencia espacio id; confirmar FK en columna FK. |
| sesiones_parqueo | estado | VARCHAR(15) | No | — | No | 'ACTIVA' | Campo estado de sesiones_parqueo. |
| sesiones_parqueo | hora_entrada | TIMESTAMPTZ | No | — | No | now() | Campo hora entrada de sesiones_parqueo. |
| sesiones_parqueo | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| sesiones_parqueo | numero_tiquete | VARCHAR(30) | No | — | No | — | Campo numero tiquete de sesiones_parqueo. |
| sesiones_parqueo | operacion_cierre_id | UUID | No | — | Sí | — | Identificador de referencia operacion cierre id; confirmar FK en columna FK. |
| sesiones_parqueo | operador_entrada_id | UUID | No | — | No | — | Identificador de referencia operador entrada id; confirmar FK en columna FK. |
| sesiones_parqueo | placa_actual | VARCHAR(10) | No | — | No | — | Campo placa actual de sesiones_parqueo. |
| sesiones_parqueo | placa_ingreso | VARCHAR(10) | No | — | No | — | Campo placa ingreso de sesiones_parqueo. |
| sesiones_parqueo | tipo_cierre | VARCHAR(15) | No | — | Sí | — | Campo tipo cierre de sesiones_parqueo. |
| sesiones_parqueo | tipo_vehiculo | VARCHAR(10) | No | espacios.tipo_vehiculo | No | — | Campo tipo vehiculo de sesiones_parqueo. |
| solicitudes_salida_excepcional | autorizacion_identity_id | UUID | No | — | Sí | — | Identificador de referencia autorizacion identity id; confirmar FK en columna FK. |
| solicitudes_salida_excepcional | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| solicitudes_salida_excepcional | estado | VARCHAR(15) | No | — | No | 'PENDIENTE' | Campo estado de solicitudes_salida_excepcional. |
| solicitudes_salida_excepcional | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| solicitudes_salida_excepcional | jti | UUID | No | — | Sí | — | Campo jti de solicitudes_salida_excepcional. |
| solicitudes_salida_excepcional | motivo | TEXT | No | — | No | — | Campo motivo de solicitudes_salida_excepcional. |
| solicitudes_salida_excepcional | operacion_id | UUID | No | — | No | — | Identificador de referencia operacion id; confirmar FK en columna FK. |
| solicitudes_salida_excepcional | operador_solicitante_id | UUID | No | — | No | — | Identificador de referencia operador solicitante id; confirmar FK en columna FK. |
| solicitudes_salida_excepcional | sesion_id | UUID | No | sesiones_parqueo.id | No | — | Identificador de referencia sesion id; confirmar FK en columna FK. |
| traslados_espacio | espacio_destino_id | UUID | No | espacios.id | No | — | Identificador de referencia espacio destino id; confirmar FK en columna FK. |
| traslados_espacio | espacio_origen_id | UUID | No | espacios.id | No | — | Identificador de referencia espacio origen id; confirmar FK en columna FK. |
| traslados_espacio | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| traslados_espacio | motivo | TEXT | No | — | No | — | Campo motivo de traslados_espacio. |
| traslados_espacio | operador_id | UUID | No | — | No | — | Identificador de referencia operador id; confirmar FK en columna FK. |
| traslados_espacio | sesion_id | UUID | No | sesiones_parqueo.id | No | — | Identificador de referencia sesion id; confirmar FK en columna FK. |
| traslados_espacio | trasladado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a trasladado en. |
| zonas | activa | BOOLEAN | No | — | No | TRUE | Campo activa de zonas. |
| zonas | actualizado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a actualizado en. |
| zonas | codigo | VARCHAR(15) | No | — | No | — | Campo codigo de zonas. |
| zonas | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| zonas | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| zonas | nombre | VARCHAR(80) | No | — | No | — | Campo nombre de zonas. |
| zonas | piso_id | UUID | No | pisos.id | No | — | Identificador de referencia piso id; confirmar FK en columna FK. |

## db_identity

| Tabla | Campo | Tipo de dato | PK | FK | Nulo | Default | Descripción |
|---|---|---|---|---|---|---|---|
| auditoria_seguridad | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| auditoria_seguridad | evento | VARCHAR(50) | No | — | No | — | Campo evento de auditoria_seguridad. |
| auditoria_seguridad | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| auditoria_seguridad | usuario_id | UUID | No | usuarios.id | Sí | — | Identificador de referencia usuario id; confirmar FK en columna FK. |
| autorizaciones_cargo_abandono | administrador_autorizador_id | UUID | No | usuarios.id | No | — | Identificador de referencia administrador autorizador id; confirmar FK en columna FK. |
| autorizaciones_cargo_abandono | autorizada_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a autorizada en. |
| autorizaciones_cargo_abandono | caso_abandono_id | UUID | No | — | No | — | Identificador de referencia caso abandono id; confirmar FK en columna FK. |
| autorizaciones_cargo_abandono | clave_idempotencia | UUID | No | — | No | — | Campo clave idempotencia de autorizaciones_cargo_abandono. |
| autorizaciones_cargo_abandono | estado | VARCHAR(15) | No | — | No | — | Campo estado de autorizaciones_cargo_abandono. |
| autorizaciones_cargo_abandono | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| autorizaciones_cargo_abandono | importe_autorizado | NUMERIC(12,2) | No | — | No | — | Valor monetario o cantidad de importe autorizado. |
| autorizaciones_cargo_abandono | moneda | CHAR(3) | No | — | No | 'COP' | Campo moneda de autorizaciones_cargo_abandono. |
| autorizaciones_cargo_abandono | motivo | TEXT | No | — | No | — | Campo motivo de autorizaciones_cargo_abandono. |
| autorizaciones_cargo_abandono | operacion_id | UUID | No | — | No | — | Identificador de referencia operacion id; confirmar FK en columna FK. |
| autorizaciones_cargo_abandono | operador_solicitante_id | UUID | No | usuarios.id | No | — | Identificador de referencia operador solicitante id; confirmar FK en columna FK. |
| autorizaciones_cargo_abandono | roles_verificados | JSONB | No | — | No | — | Campo roles verificados de autorizaciones_cargo_abandono. |
| autorizaciones_excepcionales | administrador_autorizador_id | UUID | No | usuarios.id | No | — | Identificador de referencia administrador autorizador id; confirmar FK en columna FK. |
| autorizaciones_excepcionales | emitida_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a emitida en. |
| autorizaciones_excepcionales | estado | VARCHAR(15) | No | — | No | — | Campo estado de autorizaciones_excepcionales. |
| autorizaciones_excepcionales | expira_en | TIMESTAMPTZ | No | — | No | — | Fecha/hora asociada a expira en. |
| autorizaciones_excepcionales | finalidad | VARCHAR(40) | No | — | No | — | Campo finalidad de autorizaciones_excepcionales. |
| autorizaciones_excepcionales | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| autorizaciones_excepcionales | jti | UUID | No | — | No | — | Campo jti de autorizaciones_excepcionales. |
| autorizaciones_excepcionales | motivo | TEXT | No | — | No | — | Campo motivo de autorizaciones_excepcionales. |
| autorizaciones_excepcionales | operacion_id | UUID | No | — | No | — | Identificador de referencia operacion id; confirmar FK en columna FK. |
| autorizaciones_excepcionales | operador_solicitante_id | UUID | No | usuarios.id | No | — | Identificador de referencia operador solicitante id; confirmar FK en columna FK. |
| autorizaciones_excepcionales | roles_verificados | JSONB | No | — | No | — | Campo roles verificados de autorizaciones_excepcionales. |
| autorizaciones_excepcionales | sesion_parking_id | UUID | No | — | No | — | Identificador de referencia sesion parking id; confirmar FK en columna FK. |
| autorizaciones_excepcionales | solicitud_parking_id | UUID | No | — | No | — | Identificador de referencia solicitud parking id; confirmar FK en columna FK. |
| identity_bootstrap_lock | id | SMALLINT | Sí | — | No | — | Identificador único de la fila. |
| roles | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| roles | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| roles | nombre | VARCHAR(30) | No | — | No | — | Campo nombre de roles. |
| usuario_roles | asignado_por | UUID | No | usuarios.id | Sí | — | Campo asignado por de usuario_roles. |
| usuario_roles | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| usuario_roles | rol_id | UUID | Sí | roles.id | No | — | Identificador de referencia rol id; confirmar FK en columna FK. |
| usuario_roles | usuario_id | UUID | Sí | usuarios.id | No | — | Identificador de referencia usuario id; confirmar FK en columna FK. |
| usuarios | actualizado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a actualizado en. |
| usuarios | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| usuarios | email | VARCHAR(150) | No | — | No | — | Campo email de usuarios. |
| usuarios | estado | VARCHAR(15) | No | — | No | 'ACTIVO' | Campo estado de usuarios. |
| usuarios | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| usuarios | intentos_fallidos | SMALLINT | No | — | No | 0 | Campo intentos fallidos de usuarios. |
| usuarios | nombre | VARCHAR(120) | No | — | No | — | Campo nombre de usuarios. |
| usuarios | password_hash | VARCHAR(255) | No | — | No | — | Campo password hash de usuarios. |

## db_billing

| Tabla | Campo | Tipo de dato | PK | FK | Nulo | Default | Descripción |
|---|---|---|---|---|---|---|---|
| anulaciones_liquidacion | administrador_id | UUID | No | — | No | — | Identificador de referencia administrador id; confirmar FK en columna FK. |
| anulaciones_liquidacion | clave_idempotencia | UUID | No | — | No | — | Campo clave idempotencia de anulaciones_liquidacion. |
| anulaciones_liquidacion | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| anulaciones_liquidacion | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| anulaciones_liquidacion | liquidacion_id | UUID | No | liquidaciones.id | No | — | Identificador de referencia liquidacion id; confirmar FK en columna FK. |
| anulaciones_liquidacion | motivo | TEXT | No | — | No | — | Campo motivo de anulaciones_liquidacion. |
| cajas | activa | BOOLEAN | No | — | No | TRUE | Campo activa de cajas. |
| cajas | actualizado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a actualizado en. |
| cajas | codigo | VARCHAR(20) | No | — | No | — | Campo codigo de cajas. |
| cajas | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| cajas | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| cajas | nombre | VARCHAR(80) | No | — | No | — | Campo nombre de cajas. |
| cargos_abandono | actualizado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a actualizado en. |
| cargos_abandono | administrador_autorizador_id | UUID | No | — | No | — | Identificador de referencia administrador autorizador id; confirmar FK en columna FK. |
| cargos_abandono | autorizacion_identity_id | UUID | No | — | No | — | Identificador de referencia autorizacion identity id; confirmar FK en columna FK. |
| cargos_abandono | autorizado_en | TIMESTAMPTZ | No | — | No | — | Fecha/hora asociada a autorizado en. |
| cargos_abandono | caso_abandono_id | UUID | No | — | No | — | Identificador de referencia caso abandono id; confirmar FK en columna FK. |
| cargos_abandono | clave_idempotencia | UUID | No | — | No | — | Campo clave idempotencia de cargos_abandono. |
| cargos_abandono | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| cargos_abandono | estado | VARCHAR(15) | No | — | No | — | Campo estado de cargos_abandono. |
| cargos_abandono | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| cargos_abandono | importe_autorizado | NUMERIC(12,2) | No | — | No | — | Valor monetario o cantidad de importe autorizado. |
| cargos_abandono | moneda | CHAR(3) | No | — | No | 'COP' | Campo moneda de cargos_abandono. |
| cargos_abandono | motivo | TEXT | No | — | No | — | Campo motivo de cargos_abandono. |
| comprobantes | descuentos | NUMERIC(12,2) | No | — | No | — | Valor monetario o cantidad de descuentos. |
| comprobantes | emitido_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a emitido en. |
| comprobantes | hora_corte | TIMESTAMPTZ | No | — | No | — | Campo hora corte de comprobantes. |
| comprobantes | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| comprobantes | liquidacion_id | UUID | No | liquidaciones.id | No | — | Identificador de referencia liquidacion id; confirmar FK en columna FK. |
| comprobantes | metodo_pago | VARCHAR(15) | No | — | Sí | — | Campo metodo pago de comprobantes. |
| comprobantes | numero | VARCHAR(30) | No | — | No | — | Campo numero de comprobantes. |
| comprobantes | pago_id | UUID | No | pagos.id | Sí | — | Identificador de referencia pago id; confirmar FK en columna FK. |
| comprobantes | recargos | NUMERIC(12,2) | No | — | No | — | Valor monetario o cantidad de recargos. |
| comprobantes | subtotal | NUMERIC(12,2) | No | — | No | — | Valor monetario o cantidad de subtotal. |
| comprobantes | tipo | VARCHAR(20) | No | — | No | — | Campo tipo de comprobantes. |
| comprobantes | tipo_vehiculo | VARCHAR(10) | No | — | Sí | — | Campo tipo vehiculo de comprobantes. |
| comprobantes | total | NUMERIC(12,2) | No | liquidaciones.total | No | — | Valor monetario o cantidad de total. |
| detalles_liquidacion | cantidad | NUMERIC(10,2) | No | — | No | 1 | Campo cantidad de detalles_liquidacion. |
| detalles_liquidacion | concepto | VARCHAR(30) | No | — | No | — | Campo concepto de detalles_liquidacion. |
| detalles_liquidacion | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| detalles_liquidacion | descripcion | VARCHAR(200) | No | — | No | — | Campo descripcion de detalles_liquidacion. |
| detalles_liquidacion | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| detalles_liquidacion | liquidacion_id | UUID | No | liquidaciones.id | No | — | Identificador de referencia liquidacion id; confirmar FK en columna FK. |
| detalles_liquidacion | subtotal | NUMERIC(12,2) | No | — | No | — | Valor monetario o cantidad de subtotal. |
| detalles_liquidacion | tarifa_id | UUID | No | tarifas.id | Sí | — | Identificador de referencia tarifa id; confirmar FK en columna FK. |
| detalles_liquidacion | valor_unitario | NUMERIC(12,2) | No | — | No | — | Valor monetario o cantidad de valor unitario. |
| eventos_cargo_abandono | cargo_abandono_id | UUID | No | cargos_abandono.id | No | — | Identificador de referencia cargo abandono id; confirmar FK en columna FK. |
| eventos_cargo_abandono | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| eventos_cargo_abandono | estado_nuevo | VARCHAR(15) | No | — | No | — | Campo estado nuevo de eventos_cargo_abandono. |
| eventos_cargo_abandono | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| liquidaciones | actualizado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a actualizado en. |
| liquidaciones | cargo_abandono_id | UUID | No | cargos_abandono.id | Sí | — | Identificador de referencia cargo abandono id; confirmar FK en columna FK. |
| liquidaciones | clave_idempotencia | UUID | No | — | No | — | Campo clave idempotencia de liquidaciones. |
| liquidaciones | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| liquidaciones | descuentos | NUMERIC(12,2) | No | — | No | 0 | Valor monetario o cantidad de descuentos. |
| liquidaciones | estado | VARCHAR(15) | No | — | No | 'PENDIENTE' | Campo estado de liquidaciones. |
| liquidaciones | hash_solicitud | CHAR(64) | No | — | No | — | Campo hash solicitud de liquidaciones. |
| liquidaciones | hora_corte | TIMESTAMPTZ | No | — | No | — | Campo hora corte de liquidaciones. |
| liquidaciones | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| liquidaciones | minutos_facturables | INTEGER | No | — | Sí | — | Campo minutos facturables de liquidaciones. |
| liquidaciones | minutos_gracia | SMALLINT | No | — | Sí | — | Campo minutos gracia de liquidaciones. |
| liquidaciones | minutos_totales | INTEGER | No | — | Sí | — | Valor monetario o cantidad de minutos totales. |
| liquidaciones | operador_liquida_id | UUID | No | — | No | — | Identificador de referencia operador liquida id; confirmar FK en columna FK. |
| liquidaciones | recargos | NUMERIC(12,2) | No | — | No | 0 | Valor monetario o cantidad de recargos. |
| liquidaciones | subtotal | NUMERIC(12,2) | No | — | No | — | Valor monetario o cantidad de subtotal. |
| liquidaciones | tipo_vehiculo | VARCHAR(10) | No | — | Sí | — | Campo tipo vehiculo de liquidaciones. |
| liquidaciones | total | NUMERIC(12,2) | No | — | No | — | Valor monetario o cantidad de total. |
| movimientos_caja | clave_idempotencia | UUID | No | — | No | — | Campo clave idempotencia de movimientos_caja. |
| movimientos_caja | concepto | VARCHAR(25) | No | — | No | — | Campo concepto de movimientos_caja. |
| movimientos_caja | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| movimientos_caja | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| movimientos_caja | monto | NUMERIC(12,2) | No | pagos.monto | No | — | Valor monetario o cantidad de monto. |
| movimientos_caja | operador_id | UUID | No | — | No | — | Identificador de referencia operador id; confirmar FK en columna FK. |
| movimientos_caja | pago_id | UUID | No | pagos.id | Sí | — | Identificador de referencia pago id; confirmar FK en columna FK. |
| movimientos_caja | sesion_caja_id | UUID | No | sesiones_caja.id | No | — | Identificador de referencia sesion caja id; confirmar FK en columna FK. |
| movimientos_caja | tipo_movimiento | VARCHAR(10) | No | — | No | — | Campo tipo movimiento de movimientos_caja. |
| pagos | clave_idempotencia | UUID | No | — | No | — | Campo clave idempotencia de pagos. |
| pagos | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| pagos | estado | VARCHAR(15) | No | — | No | 'PENDIENTE' | Campo estado de pagos. |
| pagos | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| pagos | liquidacion_id | UUID | No | liquidaciones.id | No | — | Identificador de referencia liquidacion id; confirmar FK en columna FK. |
| pagos | metodo | VARCHAR(15) | No | — | No | 'EFECTIVO' | Campo metodo de pagos. |
| pagos | monto | NUMERIC(12,2) | No | — | No | — | Valor monetario o cantidad de monto. |
| pagos | operador_id | UUID | No | — | No | — | Identificador de referencia operador id; confirmar FK en columna FK. |
| pagos | sesion_caja_id | UUID | No | sesiones_caja.id | Sí | — | Identificador de referencia sesion caja id; confirmar FK en columna FK. |
| parametros_facturacion | clave | VARCHAR(50) | No | — | No | — | Campo clave de parametros_facturacion. |
| parametros_facturacion | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| parametros_facturacion | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| parametros_facturacion | valor | VARCHAR(100) | No | — | No | — | Valor monetario o cantidad de valor. |
| parametros_facturacion | vigente_desde | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a vigente desde. |
| sesiones_caja | abierta_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a abierta en. |
| sesiones_caja | actualizado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a actualizado en. |
| sesiones_caja | caja_id | UUID | No | cajas.id | No | — | Identificador de referencia caja id; confirmar FK en columna FK. |
| sesiones_caja | clave_idempotencia | UUID | No | — | No | — | Campo clave idempotencia de sesiones_caja. |
| sesiones_caja | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| sesiones_caja | estado | VARCHAR(10) | No | — | No | 'ABIERTA' | Campo estado de sesiones_caja. |
| sesiones_caja | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| sesiones_caja | operador_id | UUID | No | — | No | — | Identificador de referencia operador id; confirmar FK en columna FK. |
| sesiones_caja | saldo_esperado | NUMERIC(12,2) | No | — | No | — | Valor monetario o cantidad de saldo esperado. |
| sesiones_caja | saldo_inicial | NUMERIC(12,2) | No | — | No | — | Valor monetario o cantidad de saldo inicial. |
| tarifas | activa | BOOLEAN | No | — | No | TRUE | Campo activa de tarifas. |
| tarifas | concepto | VARCHAR(25) | No | — | No | — | Campo concepto de tarifas. |
| tarifas | creado_en | TIMESTAMPTZ | No | — | No | now() | Fecha/hora asociada a creado en. |
| tarifas | id | UUID | Sí | — | No | gen_random_uuid() | Identificador único de la fila. |
| tarifas | moneda | CHAR(3) | No | — | No | 'COP' | Campo moneda de tarifas. |
| tarifas | tipo_vehiculo | VARCHAR(10) | No | — | No | — | Campo tipo vehiculo de tarifas. |
| tarifas | valor | NUMERIC(12,2) | No | — | No | — | Valor monetario o cantidad de valor. |
| tarifas | vigente_desde | TIMESTAMPTZ | No | — | No | — | Fecha/hora asociada a vigente desde. |

## Restricciones e índices

Los modelos `01_MODELO_DB_PARKING.md`, `02_MODELO_DB_IDENTITY.md` y `03_MODELO_DB_BILLING.md` documentan PK/FK, UNIQUE/CHECK, índices, triggers y permisos relevantes.
