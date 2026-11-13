# ADR-005: Idempotencia para crear u obtener liquidaciones y registrar entradas

- **Estado:** Aceptada
- **Fecha:** 13 de noviembre de 2026

## Contexto
Un reintento por falla de red no debe duplicar una entrada, una liquidación ni un cobro.

## Decisión
Las operaciones críticas reciben una clave idempotente (Idempotency-Key). Repetir la solicitud devuelve el resultado original.

## Consecuencias
Los reintentos son seguros. El cliente debe generar y conservar la clave por operación.
