# ADR-003: Comunicación REST síncrona en la primera versión

- **Estado:** Aceptada
- **Fecha:** 13 de noviembre de 2026

## Contexto
Parking necesita crear u obtener liquidaciones y confirmar el resultado de Billing durante la salida.

## Decisión
Parking se comunica con Billing mediante REST síncrono. No se usa broker de mensajes en esta versión.

## Consecuencias
Menor complejidad inicial. Se deben gestionar fallos de red y reintentos.
