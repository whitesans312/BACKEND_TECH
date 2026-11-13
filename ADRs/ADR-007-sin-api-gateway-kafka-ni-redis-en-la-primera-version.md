# ADR-007: Sin API Gateway, Kafka ni Redis en la primera versión

- **Estado:** Aceptada
- **Fecha:** 13 de noviembre de 2026

## Contexto
El alcance inicial es un parqueadero con tres servicios y un frontend. Más componentes añaden operación y complejidad.

## Decisión
La arquitectura no incluye API Gateway, broker de eventos ni caché distribuida. El frontend consume los tres servicios directamente.

## Consecuencias
Despliegue más simple. Antes de un despliegue público habrá que evaluar gateway, TLS, observabilidad y respaldos.
