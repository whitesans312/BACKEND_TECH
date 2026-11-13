# Requerimientos no funcionales

| ID | Categoría | Requisito |
|---|---|---|
| RNF-01 | Seguridad | Contraseñas almacenadas mediante hash y comunicación protegida en despliegue. |
| RNF-02 | Seguridad | Separación de credenciales de aplicación y migración de bases de datos. |
| RNF-03 | Integridad | Cada servicio es propietario de su base; no consulta tablas de otro servicio. |
| RNF-04 | Consistencia | No mantener transacciones de base de datos abiertas durante llamadas HTTP. |
| RNF-05 | Confiabilidad | Liquidaciones y cierres soportan idempotencia y reintentos controlados. |
| RNF-06 | Trazabilidad | Pagos aprobados, comprobantes y sesiones cerradas conservan su historial. |
| RNF-07 | Mantenibilidad | Esquemas versionados con Flyway y servicios independientes. |
| RNF-08 | Portabilidad | Java 21 y PostgreSQL 16. |
