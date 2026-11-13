# ParkFlow

ParkFlow está organizado como tres microservicios Spring Boot, cada uno con su propia base PostgreSQL. El frontend React consume las APIs desde el navegador.

| Componente | Puerto local | Base de datos |
| --- | ---: | --- |
| Identity | 8081 | `db_identity` |
| Parking | 8080 | `db_parking` |
| Billing | 8083 | `db_billing` |
| React | 5173 | — |
| PostgreSQL local de ParkFlow | 55433 | — |

La arquitectura no usa claves foráneas entre bases. Los cambios de operación que cruzan servicios usan JWT e idempotencia. La implementación activa está en `parkflow-db-definitivo/`. La antigua `parkflow-db/` se retiró y se respaldó fuera del repositorio. `parkflow-db-nuevo/` se conserva como referencia; los servicios actuales no la cargan.

## Iniciar el entorno local con Docker

Requisitos: Docker Desktop con Compose v2 y Java 21/Maven solo si vas a ejecutar los servicios fuera de Docker.

1. Copia `.env.parkflow.example` a `.env.parkflow`.
2. Genera las llaves RSA con `powershell -ExecutionPolicy Bypass -File .\scripts\New-ParkFlowJwtKeys.ps1`. El script guarda la llave privada PKCS#8 y la llave pública DER en Base64 dentro de `.env.parkflow`, elimina los temporales y no imprime los valores. Mantén ese archivo privado y fuera de Git.
3. Desde `backend`, inicia el stack:

```powershell
docker compose --env-file .env.parkflow -f docker-compose.parkflow.yml up --build
```

La primera inicialización crea un volumen Docker nuevo, las tres bases y roles locales, y ejecuta las migraciones con los usuarios migradores. El mapeo de PostgreSQL usa el puerto `55433` para no interferir con una instancia PostgreSQL existente en `5432`. Conserva el volumen entre reinicios. No uses `docker compose down -v` si quieres conservar los datos locales.

Abre el frontend en `http://localhost:5173`. Health checks:

- Identity: `http://localhost:8081/.well-known/jwks.json`.
- Parking: `http://localhost:8080/api/v1/health`.
- Billing: `http://localhost:8083/api/billing/health`.

La primera cuenta se crea una sola vez con:

```powershell
$body = @{ nombre = 'Administrador'; email = 'admin@parkflow.local'; password = 'CambiaEstaClave123' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/auth/register -ContentType 'application/json' -Body $body
```

La ruta bootstrap asigna `ADMINISTRADOR` únicamente si todavía no hay usuarios. Después, inicia sesión desde el frontend y usa una contraseña propia.

Para detener los servicios y mantener los datos:

```powershell
docker compose --env-file .env.parkflow -f docker-compose.parkflow.yml down
```

## Desarrollo sin Docker para Java

Primero levanta PostgreSQL con el Compose anterior. Identity necesita llaves RSA en DER/Base64. En cada servicio configura su host y base (`db_identity`, `db_parking`, `db_billing`), y habilita Flyway con el usuario migrador correspondiente antes del primer arranque (`IDENTITY_FLYWAY_ENABLED`, `PARKING_FLYWAY_ENABLED`, `BILLING_FLYWAY_ENABLED`). Mantén `ddl-auto=validate` y usa la cuenta `*_app` para ejecución normal.

Desde PowerShell, ejecuta el wrapper de cada servicio:

```powershell
Set-Location services/parking-service; .\mvnw.cmd test
Set-Location ../identity-service; .\mvnw.cmd test
Set-Location ../parking-service; .\mvnw.cmd -f ..\billing-service\pom.xml test
```

Para construir el frontend, abre una terminal desde `app/backend`:

```powershell
Set-Location ..\frontend
npm ci
npm run dev
```

Configura `VITE_IDENTITY_URL`, `VITE_PARKING_URL` y `VITE_BILLING_URL` si las APIs no están en los puertos por defecto. CORS permite por defecto `localhost:5173` y `127.0.0.1:5173`; cambia `PARKFLOW_CORS_ALLOWED_ORIGINS` para otros orígenes.

## Funcionalidad disponible en esta versión

- Identity: alta inicial de administrador, login JWT RS256, JWKS, consulta de identidad actual y control de roles.
- Parking: gestión de pisos, zonas y espacios; asignación manual e ingreso; listado/búsqueda por placa; corrección auditada de placa; creación de liquidación en Billing y cierre recuperable con la misma clave idempotente.
- Billing: tarifas e historial, cálculo de liquidaciones, cancelación de liquidaciones pendientes, cajas/sesiones de caja, pago en efectivo, movimientos y comprobantes.
- Frontend: acceso, panel operativo, ingreso con selección de espacio, búsqueda por placa, apertura de caja y flujo de cobro/salida.

Las salidas excepcionales, transferencias, mantenimiento, anulaciones, tiquete perdido, expediente completo de abandono, autorizaciones Identity y reconciliación automática aún requieren implementación. El recargo nocturno permanece sin aplicar hasta que se apruebe su regla. También está pendiente la decisión de si se exige pagar el cargo de abandono antes de entregar el vehículo.

## Estructura

- `services/parking-service`
- `services/identity-service`
- `services/billing-service`
- `parkflow-db-definitivo/{parking,identity,billing}`: migraciones Flyway activas.
- `parkflow-db-definitivo/docs`: arquitectura, datos, reglas, contratos y pendientes.
- `docker-compose.parkflow.yml`: entorno local completo.
- `../frontend`: aplicación React.
- [Estado de implementación, validaciones y límites](PARKFLOW_IMPLEMENTATION_ROADMAP.md).
- [Guía paso a paso para instalar y ejecutar ParkFlow en otro computador](Como%20instalar%20y%20hacer%20funcionar%20el%20programa/README.md).

## Entregables del primer avance

| Carpeta | Contenido |
|---|---|
| `Backlog/` | Épicas, historias de usuario, criterios de aceptación en Gherkin (`CriteriosAceptacion/*.feature`), historias técnicas, requerimientos no funcionales, `issues_github_projects.csv` y `crear_issues.sh` |
| `ADRs/` | Una decisión de arquitectura por archivo `.md` |
| `JobStories/` | Una job story por archivo `.md` |
| `JTBD/` | Jobs To Be Done por actor |
| `Secuencias/` | Diagrama de secuencia en PlantUML |
| `QueriesActions/` | Queries y Actions por servicio |
| `EventStorming/` | Proceso de event storming, bounded contexts, microservicios candidatos y patrones de interacción |

**Integrantes:** Sebastián Santos, Sebastián Urbano y Samuel Grajales.

- Backend: https://github.com/whitesans312/BACKEND_TECH
- Frontend: https://github.com/whitesans312/FRONT_TECH
- Jira: https://miniproyecto2equipo2.atlassian.net/jira/core/projects/PROD/timeline
