# 09 â€” Correcciones posteriores a la auditorÃ­a

> **Registro histÃ³rico de correcciones documentales.** Desde esta iteraciÃ³n se generaron migraciones Flyway y se implementaron los servicios. Consulte `../../PARKFLOW_IMPLEMENTATION_ROADMAP.md` para conocer los cambios de implementaciÃ³n y los vacÃ­os que siguen abiertos.

## Alcance y estado

Se incorporaron correcciones documentales derivadas de `08_AUDITORIA_FINAL_DISENO.md`. No se generÃ³ SQL ni migraciones Flyway; no se modificÃ³ Java ni se conectÃ³ o alterÃ³ ninguna base. Este documento no aprueba el diseÃ±o ni autoriza la implementaciÃ³n.

## Hallazgos atendidos

- **Cargo excepcional:** se propuso un modelo en Billing con importe autorizado, motivo, caso y autorizaciÃ³n externos, estado e historial. El cargo no registra dinero recibido.
- **Abandono y cierre:** se alineÃ³ el cierre `CERRADA/ABANDONO` con el retiro fÃ­sico, manteniendo sesiÃ³n activa y espacio ocupado durante la gestiÃ³n. Se incorporÃ³ una clave idempotente para recuperar reintentos.
- **Integridad PostgreSQL:** el documento 04 clasifica reglas por CHECK, FK, UNIQUE/Ã­ndice parcial, trigger local, transacciÃ³n/bloqueo, validaciÃ³n entre servicios y reconciliaciÃ³n. CHECK no debe consultar otras filas ni bases.
- **Identity:** se separÃ³ la autorizaciÃ³n `AUTORIZAR_CARGO_ABANDONO` de la salida excepcional gratuita, con registro/propÃ³sito y auditorÃ­a propios.
- **Responsabilidades:** se aclarÃ³ que Parking conserva sesiones/snapshots, no catÃ¡logo de vehÃ­culos; Billing diferencia cargos autorizados de transacciones reales; movimientos de caja pertenecen a Billing.
- **Pendientes:** se corrigiÃ³ la numeraciÃ³n de `07` y se agregÃ³ la versiÃ³n objetivo de PostgreSQL como decisiÃ³n pendiente.
- **PrecisiÃ³n posterior:** cargos revocados conservan historial y se propone permitir autorizaciones sucesivas con un solo cargo vigente; `LIQUIDADO` indica liquidaciÃ³n creada y `PAGADO` pago aprobado. Liquidaciones usan exactamente un origen (sesiÃ³n o cargo), y una liquidaciÃ³n de cargo no puede ser gratuita. El cierre Parking bloquea sesiÃ³n/caso y confirma retiro, cierre y clave idempotente juntos.

## Documentos modificados

- `01_ARQUITECTURA.md`: responsabilidades actualizadas; PostgreSQL objetivo pendiente.
- `02_MODELO_ENTIDAD_RELACION.md`: se refleja autorizaciÃ³n propia de abandono e historial/cobro de cargos en Billing, sin FK entre bases.
- `03_DICCIONARIO_DE_DATOS.md`: `tipo_cierre=ABANDONO`, operaciÃ³n de cierre idempotente, autorizaciÃ³n Identity y entidades Billing para cargo e historial.
- `04_REGLAS_DE_INTEGRIDAD.md`: condiciones de cierre/retiro, separaciÃ³n de UUID externos y clasificaciÃ³n del mecanismo tÃ©cnico aplicable.
- `05_COMUNICACION_ENTRE_SERVICIOS.md`: flujo de autorizaciÃ³n, cargo, retiro/cierre recuperable y pago real condicionado a polÃ­tica aprobada.
- `06_REGLAS_DE_NEGOCIO.md`: transiciÃ³n de abandono aclarada y cargo/deuda/pago separados.
- `07_DECISIONES_PENDIENTES.md`: preguntas renumeradas y decisiÃ³n de versiÃ³n PostgreSQL agregada.
- `08_AUDITORIA_FINAL_DISENO.md`: se conserva como informe de hallazgos original; sus observaciones no se borran ni se declara aprobado el diseÃ±o.

## Entidades propuestas o ajustadas

### Parking

- `casos_abandono` conserva el flujo administrativo y sus estados.
- `contactos_autoridad`, `evidencias_abandono` y `retiros_vehiculo` conservan hechos operativos.
- `sesiones_parqueo.tipo_cierre` incluye `ABANDONO`; `operacion_cierre_id` permite idempotencia local. Las referencias a Billing/Identity siguen siendo UUID externos.

### Identity

- `autorizaciones_cargo_abandono` registra por separado solicitante, administrador, importe/motivo y permiso especÃ­fico. No reutiliza automÃ¡ticamente la autorizaciÃ³n de salida excepcional.

### Billing

- `cargos_abandono` registra el cargo autorizado y su vÃ­nculo externo a caso y autorizaciÃ³n.
- `eventos_cargo_abandono` conserva cambios append-only.
- La liquidaciÃ³n/pago existente solo se relaciona con el cargo si el cobro realmente sucede; al autorizar no se crea pago, comprobante ni movimiento de caja.

## Estados y relaciones

- El expediente de abandono y la sesiÃ³n tienen ciclos distintos. La sesiÃ³n permanece `ACTIVA` mientras el expediente pasa por reporte/gestiÃ³n/contacto/coordinaciÃ³n. El retiro fÃ­sico permite el cierre `CERRADA/ABANDONO` y liberar el espacio.
- El caso se relaciona localmente con la sesiÃ³n dentro de Parking. Cargo y autorizaciÃ³n guardan UUID externos, sin FK entre bases.
- Los estados propuestos del cargo son `AUTORIZADO/REVOCADO/LIQUIDADO/PAGADO`; deben revisarse junto con la decisiÃ³n de exigibilidad. No representan por sÃ­ mismos un saldo por cobrar.
- La cuenta por cobrar, vencimiento, cobro al propietario y tratamiento del impago no se han decidido ni se generan automÃ¡ticamente.

## Decisiones aÃºn pendientes

1. Â¿Se cobra al propietario cuando reclama el vehÃ­culo o se reconoce una deuda aun si no aparece?
2. Â¿El pago es condiciÃ³n para entregar el vehÃ­culo?
3. Â¿CÃ³mo se determina el importe excepcional y quÃ© ocurre si no se recupera?
4. Recargo nocturno, reinicio de gracia y admisiÃ³n fuera del horario operativo.
5. Flujo/documento de salida gratuita; intentos de pago y recuperaciÃ³n distribuida.
6. RetenciÃ³n de placas, cardinalidad de cajas, otros movimientos de caja, apertura/cierre y conteo.
7. DuraciÃ³n/claims de autorizaciones, autoaprobaciÃ³n y recuperaciÃ³n de Identity.
8. VersiÃ³n mÃ­nima y objetivo de PostgreSQL.

## Bloqueos para aprobar migraciones

- El cargo excepcional necesita polÃ­tica aprobada de cobro/deuda y un modelo final consistente con liquidaciones.
- Debe confirmarse la propuesta de varios cargos histÃ³ricos por caso (revocados antes de liquidar) y mÃ¡ximo uno vigente; falta cerrar la reacciÃ³n ante liquidaciÃ³n cancelada/anulada.
- La opciÃ³n de exigir o no pago antes de entregar el vehÃ­culo al propietario sigue pendiente; no se condiciona la entrega desde el diseÃ±o tÃ©cnico.
- Debe decidirse cÃ³mo se relaciona cargo con liquidaciÃ³n y el pago efectivo sin introducir restricciones circulares ni permitir doble cobro.
- Los estados de cargo deben distinguir de forma verificable autorizaciÃ³n, saldo exigible y pago recibido; los estados propuestos requieren revisiÃ³n antes de congelar el esquema.
- La secuencia idempotente caso/retiro/cierre debe especificarse como una operaciÃ³n transaccional real de Parking, incluidos errores y reintentos.
- La identidad del propietario no estÃ¡ modelada; cualquier cobranza que la requiera depende de decisiÃ³n comercial y privacidad.
- PostgreSQL 16.15 se propone como objetivo provisional; falta verificar el servidor local y validar las migraciones con PG16 descartable.
- Se requiere una nueva revisiÃ³n cruzada 01â€“07 despuÃ©s de resolver estos puntos. El dictamen de `08` permanece **NO APROBADO para migraciones** hasta completar la lista de comprobaciÃ³n.

## Dictamen

Las correcciones reducen las inconsistencias documentales, pero son propuestas de diseÃ±o. Permanecen decisiones comerciales y detalles de consistencia por cerrar; por tanto, el diseÃ±o continÃºa **NO APROBADO para generar migraciones definitivas**.
