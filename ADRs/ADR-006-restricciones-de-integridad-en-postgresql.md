# ADR-006: Restricciones de integridad en PostgreSQL

- **Estado:** Aceptada
- **Fecha:** 13 de noviembre de 2026

## Contexto
Algunas reglas críticas (una sesión activa por placa y por espacio, pagos aprobados con movimiento y comprobante) no deben depender solo del código Java.

## Decisión
Se implementan índices parciales, restricciones, triggers y validaciones diferidas en cada base. Las reglas de cálculo tarifario se mantienen en Java.

## Consecuencias
La consistencia crítica queda protegida aunque falle la aplicación. Parte de la lógica vive en la base y debe versionarse con Flyway.
