# Paso 6 — Microservicios candidatos

| Candidato | ¿Se implementa? | Razón |
|---|---|---|
| Identity Service | Sí | Contexto propio con ciclo de cambio distinto (seguridad) y publica JWKS para los demás. |
| Parking Service | Sí | Concentra la operación diaria y el estado de ocupación. |
| Billing Service | Sí | Reglas tarifarias y registros financieros que requieren trazabilidad e integridad propias. |
| Notificaciones | No (por ahora) | No hay requisito de mensajería; no se justifica un servicio. |
| Reportes | No (por ahora) | Las consultas viven en cada servicio; se evaluará con el cierre de cajas. |
| Abandono de vehículos | No (pendiente) | Existe en el DDL de Parking, Identity y Billing, pero sin reglas definidas; ver hotspots. |
| API Gateway | No | Decidido en ADR-007 para la primera versión. |
