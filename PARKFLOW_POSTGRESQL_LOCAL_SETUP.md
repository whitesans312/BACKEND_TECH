# PostgreSQL local para ParkFlow

**Actualizado:** 2026-11-12.

## Aislamiento y versión

La configuración verificada usa PostgreSQL 16.15. Las bases ParkFlow son `db_parking`, `db_identity` y `db_billing`; cada una tiene roles `*_app` de ejecución y `*_migrator` para Flyway. No hay FK entre bases. El Compose de ParkFlow publica PostgreSQL en `55433`, dejando libre una instalación local que use `5432`.

Las verificaciones de esta implementación se ejecutaron en un clúster PostgreSQL temporal separado en `55432`; no se ejecutaron migraciones sobre la instancia del usuario en `5432`.

## Iniciar el entorno local

Desde `app/backend`:

1. Copia `.env.parkflow.example` a `.env.parkflow`.
2. Genera claves RS256 de desarrollo:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\New-ParkFlowJwtKeys.ps1
   ```

3. Inicia el stack:

   ```powershell
   docker compose --env-file .env.parkflow -f docker-compose.parkflow.yml up --build
   ```

Compose inicia las tres bases y sus migraciones. Puertos predeterminados: Identity `8081`, Parking `8080`, Billing `8083`, React `5173`, PostgreSQL `55433`. Para detener sin borrar datos:

```powershell
docker compose --env-file .env.parkflow -f docker-compose.parkflow.yml down
```

No uses `down -v` si deseas conservar la base local.

## Migraciones y cambios de esquema

Los POM empaquetan únicamente `parkflow-db-definitivo/`:

- Parking V1–V4.
- Identity V1–V3.
- Billing V1–V4.

Los permisos de aplicación/migración se crean por el script de infraestructura del Compose. En un servidor existente, revisa primero `parkflow-db-definitivo/infrastructure/01_create_roles_and_databases.sql`, el `.env`, el propietario de la base y el estado de datos. No apliques scripts de `parkflow-db-nuevo/` ni bases históricas junto con las definitivas.

Flyway está deshabilitado en el perfil normal. Habilítalo explícitamente para la base correcta con `*_FLYWAY_ENABLED=true` y credenciales migradoras. Mantén el usuario de aplicación sin privilegios DDL. Hibernate normal corre en `validate`.

## Primera cuenta

Identity permite crear el primer administrador por el endpoint bootstrap `/api/auth/register` solamente cuando todavía no existen usuarios. Después, autentícate desde la UI y administra cuentas/roles según permisos.

## Verificación

`docker compose ... config --quiet` comprueba el archivo Compose sin iniciar contenedores. La validación real de PostgreSQL se puede ejecutar con los tests `External*MigrationTest`, usando `PARKFLOW_TEST_JDBC_URL`, `IDENTITY_TEST_JDBC_URL` y `BILLING_TEST_JDBC_URL` con usuarios migradores de una instancia descartable. Las suites Java se ejecutan con `mvnw.cmd test` en cada servicio; los tests Testcontainers necesitan Docker disponible.

No uses una base con información real como entorno de prueba. Antes de migrar una base de trabajo o compartida, prepara backup y plan de recuperación con el responsable de la base.
