# ADR-001: Separar Identity, Parking y Billing en tres microservicios

- **Estado:** Aceptada
- **Fecha:** 13 de noviembre de 2026

## Contexto
El sistema mezcla tres dominios distintos: identidad y acceso, operación del estacionamiento y facturación. Cada uno cambia por razones diferentes.

## Decisión
Se divide el backend en tres microservicios independientes: Identity (puerto 8081), Parking (8080) y Billing (8083).

## Consecuencias
Permite separar responsabilidades y evolucionar cada servicio por su cuenta. A cambio exige contratos de comunicación entre servicios.
