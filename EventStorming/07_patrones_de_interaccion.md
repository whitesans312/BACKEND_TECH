# Paso 7 — Patrones de interacción

| Patrón | Dónde se usa | ADR | Consecuencia |
|---|---|---|---|
| Solicitud–respuesta síncrona (REST) | Frontend → servicios; Parking → Billing | ADR-003 | Simple, pero exige manejar fallas de red y tiempos de espera. |
| Orquestación desde Parking | Salida: Parking coordina con Billing | ADR-003 | El flujo de salida tiene un punto único de control; Parking depende de la disponibilidad de Billing. |
| Clave idempotente | Entradas, salidas, liquidaciones y apertura de caja | ADR-005 | Reintentos seguros; no se interpreta una respuesta desconocida como pago confirmado. |
| Validación local de token | Parking y Billing validan el JWT con la JWKS de Identity | ADR-004 | Sin consulta a Identity por petición; requiere caché y rotación de claves. |
| Transacción local en el dueño del dato | Billing: pago, movimiento y comprobante en una sola transacción | ADR-002, ADR-006 | Consistencia fuerte dentro del servicio; entre servicios hay consistencia eventual con reintento. |
| Referencia por identificador externo | UUID de sesión, autorización y liquidación entre servicios | ADR-002 | Sin claves foráneas entre bases; la validación se hace por API. |
