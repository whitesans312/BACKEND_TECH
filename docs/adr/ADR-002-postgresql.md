# ADR-002: PostgreSQL como almacén transaccional

- Estado: Aceptada (decisión observada)
- Contexto: Ventas, órdenes, pagos y movimientos requieren relaciones, integridad referencial y transacciones.
- Decisión: Persistir en PostgreSQL; la configuración contempla Supabase como servicio administrado. Spring Data JPA/Hibernate mapea entidades y repositorios. Funciones, triggers, catálogos y vistas complementan las reglas relacionales.
- Consecuencias: Restricciones y automatizaciones de esquema forman parte del comportamiento de negocio y deben desplegarse/versionarse junto con la aplicación. Existe acoplamiento a PostgreSQL en analítica y triggers.
- Evidencia: `application.properties`, `BD_ERG_POS_COMPLETA.sql`, `DATABASE_SCHEMA.md`.
