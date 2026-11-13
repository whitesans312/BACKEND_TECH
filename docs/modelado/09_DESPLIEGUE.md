# Despliegue local de ParkFlow

## Fuente

Configuración: `docker-compose.parkflow.yml`; imágenes construidas desde Dockerfiles de `services/*` y del frontend. PostgreSQL usa imagen `postgres:16.15`. No se encontró despliegue de cloud/Kubernetes en los archivos revisados.

## Contenedores y puertos

| Servicio Compose | Contenedor | Puerto interno | Puerto host por defecto | Persistencia/depende |
|---|---|---:|---:|---|
| `postgres` | `parkflow-postgres-local` | 5432 | 55433 (`PARKFLOW_PG_PORT`) | Volumen `parkflow_pg16_data`; healthcheck `pg_isready`. |
| `identity` | `parkflow-identity` | 8081 | 8081 (`IDENTITY_HTTP_PORT`) | Espera PostgreSQL saludable. |
| `parking` | `parkflow-parking` | 8080 | 8080 (`PARKING_HTTP_PORT`) | Espera PostgreSQL saludable, Identity y Billing iniciados. |
| `billing` | `parkflow-billing` | 8083 | 8083 (`BILLING_HTTP_PORT`) | Espera PostgreSQL saludable e Identity iniciado. |
| `frontend` | `parkflow-frontend` | 80 | 5173 (`FRONTEND_HTTP_PORT`) | Build de React servido por contenedor web; depende de los tres servicios. |

Compose no declara red personalizada; usa la red por defecto del proyecto. El contenedor de PostgreSQL aloja las tres bases. Los tres servicios usan su propia cuenta de aplicación y cuenta migradora, con variables distintas.

No se encontró workflow de GitHub Actions bajo `.github/workflows` en el árbol inspeccionado; la carpeta `.github` existente no contiene una configuración CI para compilar o desplegar ParkFlow.

## Configuración relevante (nombres, no valores secretos)

- PostgreSQL: `PARKFLOW_PG_PORT`, `PARKFLOW_PG_ADMIN_PASSWORD`.
- Identity: `IDENTITY_HTTP_PORT`, `IDENTITY_JWT_ISSUER`, `IDENTITY_JWT_AUDIENCE`, `IDENTITY_JWT_PRIVATE_KEY_B64`, `IDENTITY_JWT_PUBLIC_KEY_B64`, `IDENTITY_JWT_TTL`.
- Parking: `PARKING_HTTP_PORT`, `BILLING_SERVICE_URL`, issuer/audience JWKS.
- Billing: `BILLING_HTTP_PORT`, `PARKFLOW_GRACE_MINUTES`, issuer/audience/JWKS.
- Frontend build args: `VITE_IDENTITY_URL`, `VITE_PARKING_URL`, `VITE_BILLING_URL`.
- CORS se restringe a los orígenes locales configurados por `PARKFLOW_CORS_ALLOWED_ORIGINS`.

El SQL de infraestructura crea roles y bases para un clúster local vacío; declara explícitamente que sus credenciales son solo de desarrollo. No copiar credenciales de desarrollo ni archivos `.env` a un despliegue público. Compose requiere claves JWT codificadas antes de iniciar Identity.

## Carga de migraciones

Los POM de servicios incluyen el directorio de migraciones definitivo como recurso `db/migration`. Flyway se habilita en Compose mediante usuario migrador y configuración de cada servicio. La única fuente activa de migraciones es `parkflow-db-definitivo`; `parkflow-db-nuevo` se conserva como referencia histórica y no debe usarse en paralelo.

Migraciones presentes en árbol: Parking V1–V5, Identity V1–V3, Billing V1–V4. La validación temporal registrada en roadmap cubre Parking V1–V4; V5 requiere confirmación independiente.

## Dependencias y salud

Solo PostgreSQL define healthcheck explícito. Identity/Parking/Billing no tienen healthcheck Docker en Compose; `depends_on: service_started` confirma inicio del contenedor, no disponibilidad HTTP. Por tanto, el orden de inicio no es por sí solo una prueba de readiness.

## Límite del despliegue

Este Compose es local para desarrollo/revisión. No incorpora Gateway, DNS público, TLS, balanceador, escalado, logging central, métricas, backups automatizados ni política de recuperación. Para exponerlo en Internet deben definirse red/CORS, HTTPS, secretos, backup/restore y controles de acceso.
