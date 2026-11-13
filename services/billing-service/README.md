# ParkFlow Billing Service

Servicio Spring Boot independiente, propietario de tarifas, pagos y comprobantes en `db_billing`.

## Persistencia y seguridad local

Las migraciones oficiales se mantienen únicamente en `../../parkflow-db-definitivo/billing` y Maven las empaqueta como `db/migration`. Hibernate usa `ddl-auto=validate`; no crea ni actualiza tablas. Flyway permanece desactivado por defecto.

La aplicación local escucha por defecto solo en `127.0.0.1:8083` y rechaza iniciar si la dirección se cambia a una interfaz no loopback. **La autenticación/autorización aún no está implementada**: los endpoints internos y administrativos son exclusivamente de desarrollo local y no deben exponerse a una red ni desplegarse. El `operadorId` recibido en solicitudes es un UUID de auditoría no verificado; no acredita identidad. Antes de producción se debe añadir identidad de servicio y validar JWT/roles, derivando al actor de un token validado.

## Configuración local

Requiere Java 21, Maven y una instancia local con `db_billing` ya preparada según el proceso aprobado del proyecto. Billing no crea la base ni ejecuta migraciones al arrancar.

Variables disponibles:

| Variable | Valor local predeterminado |
|---|---|
| `PORT` | `8083` |
| `BILLING_SERVER_ADDRESS` | `127.0.0.1` |
| `BILLING_DB_HOST` | `localhost` |
| `BILLING_DB_PORT` | `5432` |
| `BILLING_DB_NAME` | `db_billing` |
| `BILLING_DB_USERNAME` | `billing_app` |
| `BILLING_DB_PASSWORD` | `cambiar_billing_app` (solo coincide con el bootstrap local; cambiar fuera de local) |

Flyway está fijado en `false` en el arranque normal; no se habilita con una variable genérica. Para migración explícita, preparar previamente base, rol `billing_app` y una cuenta migrator con permisos DDL; definir `BILLING_DB_MIGRATION_USERNAME` y `BILLING_DB_MIGRATION_PASSWORD`, luego arrancar con el perfil `migrate`. No usar `billing_app` como migrator. No ejecutar los scripts de conveniencia sin revisar su posible efecto destructivo.

Desde esta carpeta para las pruebas y el arranque normal:

```powershell
mvn test
mvn spring-boot:run
```

`mvn test` usa dobles y no requiere PostgreSQL. La migración explícita se ejecuta con `SPRING_PROFILES_ACTIVE=migrate` y las credenciales migrator indicadas arriba, tras autorizar/preparar la base local. No usar `billing_app` como migrator ni ejecutar scripts de conveniencia sin revisar su posible efecto destructivo.

## API local

Las rutas no están autenticadas y solo son accesibles por loopback de manera predeterminada.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/billing/health` | Liveness del proceso; no valida PostgreSQL. |
| `GET` | `/api/billing/tarifas?tipoVehiculo=CARRO` | Historial de tarifas (filtro opcional). |
| `PUT` | `/api/billing/tarifas/vigentes/{tipoVehiculo}` | Desactiva la tarifa activa y crea una nueva en una transacción local. |
| `POST` | `/internal/v1/liquidaciones` | Crea o recupera liquidación por `Idempotency-Key`. |
| `GET` | `/internal/v1/liquidaciones/by-idempotency-key/{uuid}` | Consulta por clave. |
| `GET` | `/internal/v1/pagos?sesionId={uuid}` | Consulta intentos de una sesión. |
| `POST` | `/internal/v1/pagos/{id}/resolver` | Confirma EFECTIVO o SIN_COSTO y emite comprobante atómicamente. TARJETA no está disponible. |
| `POST` | `/internal/v1/pagos/{id}/rechazar` | Rechaza un pendiente con motivo. |
| `GET` | `/api/billing/pagos/{id}` | Consulta pago y comprobante asociado. |
| `GET` | `/api/billing/comprobantes/{id}` | Consulta comprobante. |

### Ejemplos de cuerpos

Reemplazo de tarifa vigente:

```json
{
  "unidadMinutos": 60,
  "valorUnidad": 5000.00,
  "minutosGracia": 10,
  "moneda": "COP"
}
```

Crear liquidación (cabecera `Idempotency-Key` UUID):

```json
{
  "sesionId": "e1000000-0000-0000-0000-000000000001",
  "numeroTicket": "PF-2026-000001",
  "tipoVehiculo": "CARRO",
  "placa": "ABC123",
  "fechaEntrada": "2026-11-02T13:00:00Z",
  "fechaCorte": "2026-11-02T15:10:00Z",
  "operadorId": "00000000-0000-0000-0000-000000000001"
}
```

Resolver como efectivo:

```json
{
  "metodo": "EFECTIVO",
  "operadorId": "00000000-0000-0000-0000-000000000001"
}
```

El operador es local/no verificado hasta incorporar autenticación. En V1, la confirmación efectiva de recepción de efectivo es responsabilidad del proceso operador; Billing no conecta a caja ni procesador bancario. SIN_COSTO solo se admite con total cero. El esquema no define caducidad para PENDIENTE; no se cancelan automáticamente.

## Reglas implementadas

- Hash SHA-256 de UTF-8 de `sesionId|numeroTicket|placa|tipoVehiculo|fechaEntradaUTC_ISO_INSTANT`; excluye corte y operador. Misma clave y hash devuelve el original; hash distinto produce 409.
- Un pago PENDIENTE/APROBADO vigente por sesión. Solicitudes concurrentes con snapshot coincidente recuperan el intento existente tras rollback; contenido diferente produce conflicto.
- Minutos = piso de segundos/60; unidades = techo de `max(0, minutos - gracia) / unidad`; total en escala 2.
- Tarifas reemplazadas históricamente, sin reactivar/editar registros anteriores.
- Transición PENDIENTE→APROBADO/RECHAZADO solamente. Aprobación y comprobante se guardan en la misma transacción; resolución repetida idéntica devuelve el resultado existente.
- Bases y claves externas de Parking/Identity permanecen UUID sin FK entre bases.

## Limitaciones

Pruebas actuales no verifican PostgreSQL, triggers, índices parciales, permisos, `ddl-auto=validate` ni el constraint trigger diferido. Esas validaciones requieren una fase aprobada con PostgreSQL descartable. No hay integración con Parking, Identity, Gateway ni proveedor de tarjeta.
