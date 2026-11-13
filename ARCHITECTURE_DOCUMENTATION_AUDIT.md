# Auditoría de trazabilidad de la documentación ParkFlow

## 1. Alcance y método

Auditoría estática y de solo lectura para preparar documentación técnica basada en el repositorio. Se tomó el SQL Flyway como fuente de verdad del esquema. No se conectó a PostgreSQL ni se ejecutaron migraciones, scripts locales, pruebas o despliegues durante esta tarea.

## 2. Archivos y áreas inspeccionados

- Estructura backend, `README.md`, `DATABASE_SCHEMA.md` y `ARCHITECTURE_REVIEW.md`.
- `services/parking-service/pom.xml`, `Dockerfile`, Maven wrapper y `src/` (código, configuración y pruebas).
- V1/V2 SQL de `parkflow-db/parking-service`, `parkflow-db/billing-service` y `parkflow-db/identity-service`.
- SQL local de conveniencia en `parkflow-db/`, scripts de pruebas/desarrollo, query de disponibilidad y documentación `AJUSTES_V3_1_Y_RESULTADOS.md`.
- Configuración de entorno inspeccionada sin reproducir valores sensibles.

Los scripts destructivos `run_local.sh` y `run_tests.sh` se leyeron, pero no se ejecutaron.

## 3. Evidencia confirmada

| Afirmación | Evidencia |
|---|---|
| Solo Parking tiene app Java | `services/parking-service/`; no hay módulos Java Identity/Billing |
| Parking expone salud, pisos y espacios | Controladores Java bajo `services/parking-service/src/main/java` |
| Hay tres esquemas aislados | V1 por dominio bajo `parkflow-db/*/src/main/resources/db/migration/` |
| Total de diez tablas y sin FK cross-database | Migraciones V1; referencias UUID externas son columnas sin FK |
| Configuración Flyway de Parking varía por perfil | `application.properties`, perfiles dev/migrate y POM |
| Endpoints de login, sesiones y cobro no están implementados | Revisión de controladores, dependencias y clientes del servicio Java |
| Scripts locales son destructivos | Lectura de `run_local.sh` y `run_tests.sh`; no ejecución |

## 4. Migraciones examinadas

- `parkflow-db/parking-service/src/main/resources/db/migration/V1__create_initial_schema.sql`
- `parkflow-db/parking-service/src/main/resources/db/migration/V2__grants.sql`
- `parkflow-db/billing-service/src/main/resources/db/migration/V1__create_initial_schema.sql`
- `parkflow-db/billing-service/src/main/resources/db/migration/V2__grants.sql`
- `parkflow-db/identity-service/src/main/resources/db/migration/V1__create_initial_schema.sql`
- `parkflow-db/identity-service/src/main/resources/db/migration/V2__grants.sql`

También se contrastaron los scripts raíz de conveniencia, semillas V900, pruebas SQL y query de disponibilidad. No se asume que los scripts de conveniencia se hayan ejecutado.

## 5. Clasificación de información

- **Implementado:** componentes presentes en código; actualmente aplicación Parking con salud, administración de pisos y espacios.
- **Definido en SQL:** entidades, constraints, índices, secuencias, funciones, triggers y grants descritos por las migraciones. Esto no confirma estado de una base desplegada.
- **Diseñado:** ownership de tres servicios, comunicación REST, JWT/JWKS y despliegue Azure, donde se describen como arquitectura objetivo.
- **Pendiente:** aplicaciones Java Identity/Billing, vehículos/sesiones, operaciones de entrada/salida, cobros, autenticación y clientes entre servicios.
- **No verificado:** aplicación real de migraciones, disponibilidad de PostgreSQL, compilación de imágenes, despliegue Azure y resultados históricos de pruebas SQL.

## 6. Diagramas generados

- `ARCHITECTURE_AND_DATA_MODEL.md`: arquitectura con clases de estado y secuencia propuesta para salida; no asigna rutas inexistentes.
- `DATABASE_SCHEMA.md`: un ER por base, sin mezclar ownership de datos.
- `MICROSERVICES_CATALOG.md`: dependencias entre servicios; flechas futuras punteadas.

## 7. Discrepancias documentales registradas

1. El documento de esquema anterior usaba `SESIONES_PARKING`; la tabla real es `sesiones_parqueo`.
2. Billing se representaba con pago/comprobante obligatorios uno a uno. SQL permite cero o un comprobante por pago, pero requiere uno para pago aprobado al confirmar.
3. Algunos resúmenes SQL omitían checks/fórmulas presentes en migraciones. El documento de esquema actualizado se basa en V1.
4. Las migraciones V2 separan grants; scripts planos de conveniencia agrupan permisos de otra forma.
5. `ARCHITECTURE_REVIEW.md` contiene una auditoría histórica y advierte que algunas observaciones de Docker/estructura anteceden reorganizaciones; no se reutilizaron como evidencia actual sin contraste.

## 8. Verificaciones y límites

Se revisaron manualmente los fences Mermaid y las rutas documentadas contra los controladores existentes; el estado de las flechas refleja implementación o diseño. El esquema se contrastó con las migraciones, y las tres bases se mantienen en diagramas separados. No se verificó sintaxis Mermaid con un renderer ni se probó Flyway/DB en una instancia.

El informe `AJUSTES_V3_1_Y_RESULTADOS.md` declara pruebas SQL exitosas con PostgreSQL 16; el dato se registra como resultado reportado, no revalidado en esta auditoría. No se afirma que existan pruebas Java de integración PostgreSQL.

## 9. Archivos generados o actualizados

- Creados: `ARCHITECTURE_AND_DATA_MODEL.md`, `MICROSERVICES_CATALOG.md`, `ARCHITECTURE_DOCUMENTATION_AUDIT.md`.
- Actualizado: `DATABASE_SCHEMA.md`, alineado con migraciones.
- No se modificaron Java, SQL, configuración, dependencias, Dockerfiles ni frontend. No se ejecutaron operaciones sobre bases de datos.
