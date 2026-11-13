# ParkFlow — diseño y migraciones definitivas en construcción

**Estado: implementación en curso.** Las migraciones V1–V3 se encuentran en `parking/`, `identity/` y `billing/`; se validaron en PostgreSQL temporal. Las decisiones comerciales señaladas en `docs/07_DECISIONES_PENDIENTES.md` siguen pendientes; no habilitan cobros o flujos que no estén definidos.

Este paquete contiene un modelo relacional nuevo y migraciones Flyway para `db_parking`, `db_identity` y `db_billing`. La infraestructura de creación está separada de las migraciones por servicio. El Compose local está en `../docker-compose.parkflow.yml`.

## Documentos

1. [Arquitectura](docs/01_ARQUITECTURA.md)
2. [Modelo entidad-relación](docs/02_MODELO_ENTIDAD_RELACION.md)
3. [Diccionario de datos](docs/03_DICCIONARIO_DE_DATOS.md)
4. [Reglas de integridad](docs/04_REGLAS_DE_INTEGRIDAD.md)
5. [Comunicación entre servicios](docs/05_COMUNICACION_ENTRE_SERVICIOS.md)
6. [Reglas de negocio](docs/06_REGLAS_DE_NEGOCIO.md)
7. [Decisiones pendientes](docs/07_DECISIONES_PENDIENTES.md)

## Estado vigente

Este directorio es la fuente de migraciones empaquetada por los tres servicios. `parkflow-db-nuevo/` se conserva como material histórico y no debe ejecutarse junto con estas migraciones. Se validaron las versiones activas en PostgreSQL 16.15 temporal: Parking V1–V4, Identity V1–V3 y Billing V1–V4. Ver el [estado de implementación](../PARKFLOW_IMPLEMENTATION_ROADMAP.md) para alcance, pruebas y límites actuales.

## Principios para la siguiente etapa

Una base por servicio; usuarios PostgreSQL distintos; sin FK entre bases; migraciones Flyway versionadas por servicio; importes `NUMERIC`; instantes `TIMESTAMPTZ`; UUID; tablas y columnas en español `snake_case`. Las transacciones locales de Billing deben completar el cobro; las coordinaciones entre bases deben usar APIs idempotentes y recuperación, no transacciones distribuidas.

## Entorno local de desarrollo

Las migraciones se comprobaron en PostgreSQL **16.15** sobre un clúster temporal. La instancia existente del usuario en puerto `5432` no se modificó. El Compose de ParkFlow usa `55433` para desarrollo y el esquema de instalación está documentado en el [README principal](../README.md).

Desde `app/backend`:

```powershell
docker compose -f docker-compose.parkflow.yml up -d
```

El script de inicialización crea tres bases independientes y seis roles con contraseñas de desarrollo. El volumen es persistente. No se incluye ningún comando que borre el volumen o datos. En entornos compartidos/producción se deben establecer contraseñas gestionadas fuera del repositorio y aplicar el aprovisionamiento revisado por el DBA.

Para migrar una base, desde la carpeta del servicio, configurar `SPRING_PROFILES_ACTIVE=migrate` y las variables `*_DB_MIGRATION_USERNAME`/`*_DB_MIGRATION_PASSWORD`, y ejecutar `.\mvnw.cmd spring-boot:run` (Billing puede usar el wrapper de Parking con `-f ..\billing-service\pom.xml`). Ejecutar el perfil de migración una vez para cada servicio/base. `ddl-auto=none` en ese perfil evita que la aplicación intente crear tablas; el perfil normal usa `validate` una vez adaptado el modelo JPA.

**Nota:** no apuntes el perfil de migración a una base con datos preexistentes sin revisar antes el plan de respaldo/migración. Los flujos de aplicación pendientes están enumerados en el roadmap; la existencia de una tabla no implica que su proceso funcional ya esté disponible.

