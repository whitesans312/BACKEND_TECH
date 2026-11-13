# ADR-004: JWT RS256 y claves públicas mediante JWKS

- **Estado:** Aceptada
- **Fecha:** 13 de noviembre de 2026

## Contexto
Parking y Billing deben validar la identidad del usuario sin consultar a Identity en cada petición.

## Decisión
Identity firma los JWT con RS256 y publica sus claves públicas en /.well-known/jwks.json. Los otros servicios validan la firma localmente.

## Consecuencias
Se evita una consulta a Identity por cada petición. Requiere manejar la rotación y el caché de claves.
