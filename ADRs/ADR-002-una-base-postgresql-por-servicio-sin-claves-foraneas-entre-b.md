# ADR-002: Una base PostgreSQL por servicio, sin claves foráneas entre bases

- **Estado:** Aceptada
- **Fecha:** 13 de noviembre de 2026

## Contexto
Los servicios deben ser dueños de sus datos para no acoplarse por esquema.

## Decisión
Cada servicio usa su propia base (db_parking, db_identity, db_billing). Las referencias entre servicios son UUID externos, no claves foráneas.

## Consecuencias
Aislamiento de datos. Las referencias externas se validan mediante las APIs y no mediante integridad referencial de la base.
