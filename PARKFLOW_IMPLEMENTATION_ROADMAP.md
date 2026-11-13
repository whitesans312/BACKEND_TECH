# ParkFlow — estado de implementación

**Actualizado:** 2026-11-12. Este documento reemplaza el diagnóstico estático anterior, que describía un estado ya superado.

## Arquitectura activa

ParkFlow tiene tres servicios Spring Boot independientes y tres bases PostgreSQL, sin claves foráneas entre bases. Las migraciones activas están en `parkflow-db-definitivo/`; `parkflow-db-nuevo/` se conserva como entrada histórica y no es la fuente que cargan los servicios.

| Servicio | Estado implementado | Límites actuales |
|---|---|---|
| Parking | Pisos, espacios, ingreso manual, tiquetes, búsqueda/corrección de placa, salida coordinada con Billing, recuperación idempotente. | Traslados, mantenimiento, anulación y salida excepcional con autorización, y expediente completo de abandono/retiro aún no están implementados como flujos de aplicación. |
| Identity | Usuarios y roles, login, JWT RS256/JWKS, bootstrap del primer administrador, gestión administrativa de cuentas. | Consumo idempotente de autorizaciones excepcionales y de cargos por abandono aún no está integrado con Parking/Billing. |
| Billing | Tarifas, cálculo, liquidaciones idempotentes, caja/sesiones, pagos en efectivo, movimientos y comprobantes; pago local atómico. | Cargo excepcional por abandono e historial de autorizaciones/revocaciones no expuestos como flujo completo. El usuario aún no definió si el pago es obligatorio para entregar el vehículo. |
| Frontend | Interfaz ParkFlow React/TypeScript para acceso, operación, configuración y caja. | No es una suite E2E ni sustituye autorización del backend. |
| Gateway | No implementado. | El navegador llama a servicios por URL/puerto configurados; despliegue requiere controles de red y CORS adecuados. |

## Migraciones y ejecución

- `db_parking`: Flyway V1–V4 (`V4` agrega idempotencia para entradas).
- `db_identity`: Flyway V1–V3.
- `db_billing`: Flyway V1–V4 (`V4` corrige el trigger diferido de integridad del pago).
- Roles de ejecución y migración separados por base. Sin FK cross-database.
- El `docker-compose.parkflow.yml` publica PostgreSQL en `55433` para evitar interferir con una instalación preexistente en `5432`.
- Las migraciones y llamadas reales descritas abajo se validaron en un clúster PostgreSQL temporal separado, puerto `55432`; no se migró ni modificó la instancia PostgreSQL del usuario en `5432`.

## Reglas tarifarias implementadas

- COP, zona `America/Bogota`, 15 minutos de gracia inicial.
- La gracia se descuenta antes de determinar tiempo facturable.
- Hasta 180 minutos facturables se aplican bloques de reloj tocados por hora o fracción. Si el tiempo facturable supera 180 minutos, la tarifa diaria reemplaza el total por horas.
- Tarifa diaria por ciclo: carro $18.000 y moto $10.000; cada ciclo tocado se cobra completo, sin prorrateo.
- Horas: carro $6.000, moto $3.500. Recargo configurado: carro $15.000 y moto $8.000.
- La aplicación del recargo nocturno sigue pendiente de definición comercial; no debe activarse como regla automática hasta aprobar horario, frecuencia y tratamiento de ingresos fuera de horario.

## Validación ejecutada

- Suites Maven de Parking, Identity y Billing: pasaron (sin fallos ni errores). Los tests Testcontainers se omiten en esta máquina porque no hay daemon Docker.
- Pruebas Flyway contra PostgreSQL 16.15 temporal: Parking V1–V4, Identity V1–V3 y Billing V1–V4 validadas; las tres bases estaban actualizadas y Flyway no detectó cambios pendientes.
- Build de producción del frontend (`npm run build`): pasó.
- Flujo HTTP real en el clúster temporal: entrada, liquidación por $6.000, aprobación en efectivo, una fila de pago aprobado, un movimiento de caja, un comprobante, cierre de Parking y reintento idempotente 200 sin duplicar filas.
- Confirmado además por API: se cerró una sesión de caja con un vehículo aún activo en Parking, y se canceló una liquidación pendiente conservando activa la sesión y su espacio.
- La cobertura de concurrencia y de todos los flujos no equivale a validación de producción. Los tests de Testcontainers no se ejecutaron por ausencia de Docker.

## Decisiones comerciales pendientes

1. Aplicación del recargo nocturno después de las 22:00, cobros repetidos y recepción de vehículos fuera de horario.
2. Salidas gratuitas: constancia siempre/a solicitud, carácter fiscal u operativo y necesidad de caja abierta. El flujo actual genera comprobante operativo para total cero.
3. Cargo por abandono: importe y criterio; pago obligatorio para entrega; manejo si el propietario no aparece. No se crea pago ni deuda automática con la información actual.
4. Prohibición de autoaprobación y duración/claims de autorizaciones excepcionales.
5. Política de retención/anonimización de placas, límite de cajas abiertas, egresos/ajustes de caja y arqueo por denominaciones.

## Próximos bloques de implementación

1. Completar entidades, APIs y transacciones de traslados, mantenimiento, anulaciones, salidas excepcionales y abandono/retiro físico.
2. Implementar las autorizaciones Identity con consumo de un solo uso y reconciliación idempotente entre servicios.
3. Añadir cargo de abandono en Billing una vez acordadas las decisiones de cobro, conservando autorizaciones revocadas.
4. Incorporar pruebas PostgreSQL de concurrencia, restricciones, fallo de red, repetición simultánea y reconciliación; instalar/usar Docker para levantar los tests Testcontainers.
5. Decidir si se necesita Gateway antes de despliegue y fijar contratos/versionado, observabilidad, secretos, backups y recuperación.
6. Actualizar cada documento 01–09 para reflejar este modelo implementado y quitar referencias a V1/V2, a dos servicios o a código aún inexistente.

## Referencias

- Diseño: `parkflow-db-definitivo/docs/01_ARQUITECTURA.md`–`09_CORRECCIONES_POST_AUDITORIA.md`.
- Migraciones: `parkflow-db-definitivo/{parking,identity,billing}/`.
- Ejecución local: `README.md`, `PARKFLOW_POSTGRESQL_LOCAL_SETUP.md` y `.env.parkflow.example`.
- Decisiones pendientes: `parkflow-db-definitivo/docs/07_DECISIONES_PENDIENTES.md`.
