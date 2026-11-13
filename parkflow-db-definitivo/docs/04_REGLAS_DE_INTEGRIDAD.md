# 04 â€” Reglas de integridad e Ã­ndices

> La implementaciÃ³n ejecutable de restricciones e Ã­ndices estÃ¡ en las migraciones Flyway versionadas. Este documento expresa el modelo completo, incluidos flujos de autorizaciÃ³n/abandono que todavÃ­a no estÃ¡n expuestos completamente por los servicios.

Este documento expresa la intenciÃ³n del modelo, no SQL listo para ejecutar. Las migraciones deberÃ¡n implementar y validar estas reglas en PostgreSQL de la versiÃ³n que se acuerde.

## Restricciones relacionales y CHECK

### Parking

- PK UUID por fila; FK estrictamente locales; `RESTRICT` para registros referenciados histÃ³ricamente.
- Placa y tipo son snapshots de sesiÃ³n, no un catÃ¡logo `vehiculos`. Ãndice Ãºnico parcial de `placa_actual` impide dos sesiones activas para la misma placa. Tipo de sesiÃ³n compatible con tipo de espacio mediante FK compuesta o validaciÃ³n transaccional reforzada.
- Un estado `ACTIVA` no lleva hora de salida ni campos de cierre. `CERRADA` requiere hora, operador de salida y confirmaciÃ³n del tipo de cierre. `ANULADA` requiere fila hija en `anulaciones_sesion`.
- `tipo_cierre=ABANDONO` requiere que el caso local estÃ© en `VEHICULO_RETIRADO`, tenga fila de retiro con fecha/responsable, y que sesiÃ³n y caso se cierren en una transacciÃ³n local idempotente. Antes de esa confirmaciÃ³n la sesiÃ³n sigue `ACTIVA` y conserva ocupado el espacio. La salida fÃ­sica y el cierre administrativo se registran en entidades distintas.
- `tipo_cierre=ABANDONO` requiere caso local asociado en estado `VEHICULO_RETIRADO` o `CERRADO` y registro de retiro fÃ­sico; mientras no se registre el retiro, la sesiÃ³n sigue `ACTIVA` y el espacio ocupado. El caso conserva su propio historial y no crea una FK circular desde sesiÃ³n.
- Un caso de abandono pertenece a una Ãºnica sesiÃ³n. Contactos de autoridad y evidencias son append-only. La transiciÃ³n de estados se valida y audita; no se borra ni reescribe el reporte original.
- `PAGADA` exige referencia externa a confirmaciÃ³n de Billing; `GRATUITA` exige confirmaciÃ³n de liquidaciÃ³n cero; `EXCEPCIONAL` exige salida excepcional local autorizada y sin pago aprobado. La comprobaciÃ³n externa se hace antes de cerrar, por API; no fingir una FK cross-database.
- `sesiones_parqueo` no tiene FK a `anulaciones_sesion` ni `salidas_excepcionales`. Cada fila hija tiene FK local a sesiÃ³n y `UNIQUE(sesion_id)` cuando aplica. Un constraint trigger diferido comprueba que sesiÃ³n anulada/cerrada excepcionalmente tenga exactamente su evidencia hija al COMMIT. No se crea dependencia FK circular.
- Para abandono, `casos_abandono.sesion_id` y `retiros_vehiculo.caso_abandono_id` son FKs locales; las referencias a Identity/Billing son UUID de contrato. Las transiciones caso/sesiÃ³n se coordinan en Parking con bloqueo de sesiÃ³n/caso y clave idempotente; no hay FK circular.
- No se deshabilita un espacio con sesiÃ³n activa. No se inicia sesiÃ³n en espacio deshabilitado/en mantenimiento. Estas operaciones deben bloquear filas compatibles para serializar la carrera con la asignaciÃ³n.
- Un traslado actualiza la sesiÃ³n y aÃ±ade su historial en una transacciÃ³n. Mantenimiento activo debe ser compatible con el estado del espacio; preferir un servicio que cambie ambos dentro de la misma transacciÃ³n y reconciliaciÃ³n.
- Placas dejan de ser obligatorias tras el plazo de retenciÃ³n aprobado; la anonimizaciÃ³n de sesiones terminales/eventos se ejecuta solo por proceso privilegiado auditado, nunca por el rol normal de aplicaciÃ³n. El plazo no se fija en este diseÃ±o.

### Identity

- Email normalizado y Ãºnico; roles permitidos por catÃ¡logo; PK compuesta impide asignaciones repetidas.
- JTI Ãºnico, expiraciÃ³n posterior a emisiÃ³n, estado coherente con `consumida_en`. `operacion_id` y `sesion_parking_id` quedan escritos en el mismo update que consume el JTI. Update condicional serializa consumo concurrente. Repetir JTI con el mismo par devuelve el resultado persistido; distinto par es conflicto y nunca cambia la asociaciÃ³n.
- Solicitante debe tener `OPERADOR` al crear la solicitud y autorizador `ADMINISTRADOR` al aprobarla. Se registran actor, acciÃ³n y roles/permisos verificados en cada evento, aunque el mismo usuario tenga ambos roles. La separaciÃ³n de funciones para autoaprobaciÃ³n queda pendiente.
- AuditorÃ­a de seguridad append-only. Excluir contraseÃ±a, JWT completo, clave privada y otros secretos de `detalle`.
- Un usuario administrador vÃ¡lido debe tener rol administrador; la asignaciÃ³n de roles se comprueba dentro de Identity. UUID de actor enviado por el cliente no es prueba de identidad.

### Billing

- `total = subtotal + recargos - descuentos`; importes no negativos y moneda COP. No hay cÃ¡lculo tarifario dentro del CHECK.
- LiquidaciÃ³n pendiente puede pasar a cancelada o anulada antes del pago aprobado; `PAGADA` es terminal. CHECK/trigger debe impedir toda transiciÃ³n de `PAGADA` a otro estado y la creaciÃ³n de una anulaciÃ³n para ella.
- El pago solo se inserta contra liquidaciÃ³n `PENDIENTE`, con `monto = liquidaciones.total > 0`; al aprobar bloquea/verifica liquidaciÃ³n y caja abierta. Para cambiar liquidaciÃ³n a `CANCELADA`, `ANULADA` o `GRATUITA`, no puede quedar intento `PENDIENTE` ni pago `APROBADO`. `GRATUITA` requiere total cero y ningÃºn pago.
- Pago aprobado debe tener total exacto, una sesiÃ³n de caja abierta, un Ãºnico movimiento de ingreso de mismo monto en esa sesiÃ³n y un Ãºnico comprobante de la misma liquidaciÃ³n e importe.
- FKs compuestas: movimiento `(pago_id,liquidacion_id,sesion_caja_id,monto)` referencia la combinaciÃ³n correspondiente de pago; comprobante `(pago_id,liquidacion_id,total)` referencia pago/liquidaciÃ³n/monto. `UNIQUE(pago_id)` en movimiento y comprobante limita a uno.
- Constraint triggers diferidos al COMMIT exigen que cada pago aprobado tenga ambos registros y que ningÃºn comprobante `PAGO_EFECTIVO` o movimiento de pago pertenezca a un pago no aprobado. La transacciÃ³n Billing incluye aprobaciÃ³n, liquidaciÃ³n `PAGADA`, movimiento, saldo esperado y comprobante.
- Impedir UPDATE/DELETE de pago aprobado, comprobante y movimiento para el rol app con privilegios y triggers; cambios administrativos requieren un procedimiento auditado, no ediciÃ³n directa.
- No cerrar sesiÃ³n de caja con pagos pendientes asociados; la asociaciÃ³n de caja se fija al aprobar. Cerrar caja no consulta ni bloquea sesiones de Parking. SesiÃ³n cerrada terminal.
- `GRATUITA`: total cero, sin fila en `pagos` ni movimiento; se registra por liquidaciÃ³n y se confirma a Parking. Si se aprueba una constancia, es documento vinculado a liquidaciÃ³n con `tipo=SALIDA_GRATUITA`, pago nulo y total cero; no se simula efectivo.
- Una liquidaciÃ³n cancelada no produce movimientos ni cierra Parking. Otra solicitud puede generar nueva liquidaciÃ³n con nueva clave; la clave anterior permanece idempotente.
- `cargos_abandono` solo registra cargo autorizado y su historial. CHECK valida el importe de la fila; FK/UNIQUE locales protegen eventos y asociaciones internas. La referencia a caso/autorizaciÃ³n de otros servicios se valida por contrato y reconciliaciÃ³n. Crear una cuenta por cobrar se pospone hasta decisiÃ³n comercial; pago/movimiento/comprobante solo se generan ante cobro real.
- Se conserva cada autorizaciÃ³n como fila de cargo independiente (`UNIQUE(autorizacion_identity_id)`); Ã­ndice Ãºnico parcial permite un solo cargo vigente `AUTORIZADO/LIQUIDADO` por caso. Revocar un cargo ya liquidado se prohÃ­be; primero la liquidaciÃ³n debe resolverse conforme a su propio flujo. `LIQUIDADO` significa liquidaciÃ³n creada; solo el pago aprobado permite pasar a `PAGADO`.
- Una liquidaciÃ³n tiene exactamente un origen: sesiÃ³n Parking XOR cargo abandono, con CHECK local sobre ambas columnas. Si el origen es cargo, CHECK impide `GRATUITA`; una liquidaciÃ³n cancelada/anulada puede reemplazarse segÃºn Ã­ndice parcial/idempotencia, pero solo puede existir un resultado de pago aprobado por cargo.
- El importe persistido en Billing debe coincidir con la autorizaciÃ³n Identity verificada en la solicitud autenticada; se compara en la transacciÃ³n del servicio y se conserva como snapshot. No es una FK ni CHECK cross-database.

## ClasificaciÃ³n de mecanismos

- **CHECK:** dominio y coherencia entre columnas de una fila (estados permitidos, importe no negativo, moneda COP, fechas en orden). No consulta otras filas, tablas ni bases.
- **FK:** existencia referencial dentro de la misma base. Los UUID externos no llevan FK.
- **UNIQUE / Ã­ndice Ãºnico parcial:** unicidad global local o por subconjunto estable, por ejemplo una sesiÃ³n activa por placa/espacio o un Ãºnico cargo por caso.
- **Trigger local:** invariantes que necesitan inspeccionar o auditar varias filas de la misma base al insertar/cambiar estados; debe ejecutarse dentro de la transacciÃ³n y cubrir todas las rutas de escritura. Un constraint trigger diferido puede validar evidencia local al COMMIT.
- **TransacciÃ³n y bloqueo de servicio:** serializan flujos multi-fila y evitan carreras; combinar con Ã­ndices/constraints como Ãºltima defensa. Aplicable a asignar espacio, aprobar pago y cerrar sesiÃ³n.
- **ValidaciÃ³n entre microservicios:** contratos REST autenticados, claves idempotentes y consulta del resultado; no es una restricciÃ³n PostgreSQL y puede sufrir timeout parcial.
- **ReconciliaciÃ³n:** lectura periÃ³dica entre referencias externas/estados para detectar operaciones incompletas y reintentar la misma operaciÃ³n. No reemplaza consistencia local ni corrige automÃ¡ticamente historias financieras.

Las reglas anteriores que hablan de `CHECK/trigger` para condiciones de pago, sesiÃ³n de caja, fila hija o cargo deben interpretarse mediante la clasificaciÃ³n anterior. Nunca implementar un CHECK que lea otra tabla. PostgreSQL 16.15 es el objetivo provisional; verificar el servidor local y ejecutar estas migraciones contra PostgreSQL 16 antes de uso real.

En el cierre por abandono, Parking bloquea sesiÃ³n y caso en orden estable. En una sola transacciÃ³n registra el retiro fÃ­sico (Ãºnico por caso), transiciona caso a `CERRADO`, sesiÃ³n a `CERRADA/ABANDONO` y guarda `operacion_cierre_id`. La repeticiÃ³n de la misma clave devuelve el resultado persistido; otra clave contra sesiÃ³n terminal es conflicto. La liberaciÃ³n del espacio se deriva del estado de sesiÃ³n y no inserta una segunda liberaciÃ³n. Errores antes del commit no dejan retiro/cierre parcial.
- El cargo excepcional por abandono requiere importe, motivo y autorizaciÃ³n administrativa trazable. Su autorizaciÃ³n no inserta `pagos`, `movimientos_caja` ni `comprobantes`; solo una cobranza realmente realizada puede entrar al flujo ordinario de Billing. La polÃ­tica de vencimiento/cobro queda pendiente.

## Ãndices recomendados

### Parking

- Ãšnicos: `pisos(numero)`, `pisos(codigo)`, `zonas(piso_id,codigo)`, `espacios(zona_id,codigo)`, `sesiones_parqueo(numero_tiquete)`.
- Ãšnicos parciales: sesiones `ACTIVA` por `placa_actual` normalizada y por `espacio_id`; solicitudes excepcionales `PENDIENTE` por sesiÃ³n; mantenimiento `EN_PROCESO` por espacio.
- Consultas: `sesiones_parqueo(estado,hora_entrada DESC)`, `(placa_ingreso,hora_entrada DESC)`, `espacios(zona_id,estado,tipo_vehiculo)`, eventos `(sesion_id,creado_en DESC)`, mantenimiento `(espacio_id,iniciado_en DESC)`, traslados `(sesion_id,trasladado_en DESC)`. El Ã­ndice por placa actual para sesiones activas es parcial y Ãºnico.

### Identity

- Ãšnico en `lower(email)`; `usuario_roles(rol_id,usuario_id)` para listar por rol; autorizaciones `(estado,expira_en)`; auditorÃ­a `(usuario_id,creado_en DESC)` y `(evento,creado_en DESC)`.

### Billing

- Liquidaciones: Ãºnico idempotencia; Ã­ndice parcial una `PENDIENTE` por sesiÃ³n Parking; `(estado,creado_en DESC)` y `(placa,creado_en DESC)`.
- Pagos: Ãºnico idempotencia; parcial una aprobaciÃ³n por liquidaciÃ³n; parcial un intento pendiente por liquidaciÃ³n si se confirma polÃ­tica de un intento abierto; `(estado,creado_en DESC)`.
- Caja: parciales una sesiÃ³n abierta por caja; la regla de una por operador queda por decidir. Movimientos `(sesion_caja_id,creado_en DESC)`; detalles por liquidaciÃ³n.
- Tarifas: bÃºsqueda por `(tipo_vehiculo,concepto,vigente_desde DESC)`. Prohibir vigencias solapadas con mecanismo compatible con PostgreSQL, por ejemplo una exclusiÃ³n sobre rango temporal y tipo/concepto si se acepta `btree_gist`, o trigger transaccional con bloqueo por clave.
- AutorizaciÃ³n Identity: Ãºnico `jti`, Ãºnico `operacion_id` si cada operaciÃ³n genera una autorizaciÃ³n, e Ã­ndice por `(estado,expira_en)`; operaciÃ³n/sesiÃ³n consumidas se consultan por ambos campos.

## Permisos

- Revocar `CONNECT` a `PUBLIC` y otorgar conexiÃ³n solo a roles autorizados en cada base cuando se requiera aislamiento fuerte.
- `*_app`: `CONNECT`, `USAGE` de esquema, DML mÃ­nimo, lectura/uso de secuencias; sin `CREATE`, sin DDL. DELETE denegado para histÃ³rico. Permiso puntual para reemplazar filas de `usuario_roles` o flujo lÃ³gico alternativo.
- `*_migrator`: dueÃ±o de objetos/migraciones en su base o rol controlado con membresÃ­a explÃ­cita; no reutilizar credenciales de app. Definir `ALTER DEFAULT PRIVILEGES` para objetos futuros del mismo creador.
- `PUBLIC` no conserva acceso accidental a esquema/tablas. `search_path` no debe aceptar esquemas no confiables.

