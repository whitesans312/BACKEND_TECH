# ParkFlow — bases de datos PostgreSQL

Paquete inicial para tres microservicios con bases independientes:

- `db_parking`: pisos, zonas, espacios, vehículos, sesiones, anulaciones, excepciones, mantenimiento y bitácora operativa.
- `db_identity`: usuarios, roles, autorizaciones excepcionales y auditoría de seguridad.
- `db_billing`: parámetros, tarifas, liquidaciones, detalle de liquidación, cajas, sesiones de caja, pagos, movimientos, comprobantes y anulaciones.

## Arquitectura y convenciones

- PostgreSQL; UUID como PK; nombres en español y `snake_case`.
- Sin claves foráneas entre bases. Los UUID externos son referencias de contrato REST.
- Parking es dueño de la ocupación y sesiones; Identity de usuarios/autorizaciones; Billing de tarifas, liquidaciones y caja.
- La aplicación Java aplica reglas temporales y coordina llamadas entre servicios. No hay transacciones distribuidas.
- Zona horaria operativa: `America/Bogota`.
- No se registra entidad cliente ni vehículos permanentes de trabajadores.
- No se permiten pagos parciales, reembolsos ni métodos distintos de efectivo.
- Las sesiones de caja pueden cerrarse aunque existan vehículos estacionados; el cobro de salida se realiza desde una sesión de caja abierta posterior.

## Instalación

1. Ejecutar `infrastructure/01_create_roles_and_databases.sql` con un usuario administrador de PostgreSQL.
2. Configurar contraseñas para los seis roles fuera del repositorio (gestor de secretos o `ALTER ROLE ... PASSWORD ...`).
3. Conectarse a cada base como su usuario migrador y ejecutar, en orden, `V1`, `V2`, `V3` de su carpeta:
   - `parking/` en `db_parking` como `parking_migrator`.
   - `identity/` en `db_identity` como `identity_migrator`.
   - `billing/` en `db_billing` como `billing_migrator`.
4. Configurar cada microservicio para conectarse solo a su propia base y con su usuario `*_app`.

Los archivos se entregan como SQL ejecutable manualmente. Para incorporarlos a Flyway, cada V1/V2/V3 se copia a la ruta `src/main/resources/db/migration/` de su servicio, respetando el orden. Ejecutar primero en una instancia local/aislada y validar antes de apuntar a cualquier entorno con datos.

## Tarifas y reglas

Se insertan los valores que el negocio ya indicó:

| Concepto | Carro | Moto |
|---|---:|---:|
| Hora o fracción | $6.000 | $3.500 |
| Día | $18.000 | $10.000 |
| Recargo nocturno | $15.000 | $8.000 |

También se insertan 15 minutos de gracia, apertura 08:00 y cierre 22:00. Los conceptos tarifarios se almacenan separados; el cálculo y su desglose se realizan en Java y se conservan en `liquidaciones` y `detalles_liquidacion`.

**Reglas que siguen requiriendo una definición comercial antes de cerrar el cálculo Java:**

1. Si el recargo nocturno se cobra por cada noche o una sola vez por permanencia.
2. Si la tarifa diaria se aplica por bloques de 24 horas o por día calendario.
3. Si al superar 3 horas la tarifa diaria reemplaza el cobro por hora o se acumula bajo una fórmula específica.
4. Cómo se cobra una permanencia de varias noches y cómo se reinicia el ciclo a las 08:00.

La estructura permite almacenar los tres conceptos sin codificar prematuramente una fórmula que todavía no está definida.

## Decisiones técnicas que deben respetarse en backend

- La asignación de espacio debe ser transaccional; los índices únicos parciales impiden dos sesiones activas en el mismo vehículo o espacio.
- La ocupación se deriva de sesiones `ACTIVA`, no de un contador persistido.
- Correcciones de placa, pérdida de tiquete y traslados deben registrar eventos en `eventos_operativos`; el traslado además conserva origen/destino en `traslados_espacio`.
- Perder el tiquete no crea un vehículo nuevo: se busca por placa, se verifica tarjeta de propiedad y se continúa la salida normal.
- Una solicitud de salida/liquidación puede cancelarse si no se concreta el pago; una nueva solicitud puede crear otra liquidación, pero solo una puede estar pendiente por sesión.
- El pago aprobado, el cambio de estado de liquidación, el comprobante y el movimiento de caja deben coordinarse en una única transacción local de Billing. El código de servicio debe actualizar `saldo_esperado` en esa misma transacción.
- No cerrar ni liberar la sesión Parking hasta confirmar el pago en Billing.
- Una salida excepcional debe contar con autorización administrativa de Identity y quedar registrada en Parking. No se representa como pago `SIN_COSTO`.
- No se borran registros históricos ni pagos aprobados. No se modelan devoluciones.
- Los UUID de operador/administrador son referencias externas a Identity; no tienen FK local.

## Alcance y estado

Este es un **primer paquete de esquema SQL**, no una certificación de producción. No se ha ejecutado contra una instancia PostgreSQL en este entorno. Antes de usarlo, validar sintaxis/migraciones en PostgreSQL, probar restricciones y transacciones, revisar los permisos efectivos y cerrar las reglas de tarifa pendientes. No ejecutar el script de infraestructura sobre una instancia compartida sin revisar nombres y permisos existentes.
