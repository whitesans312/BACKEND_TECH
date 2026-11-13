# Paso 5 — Bounded Contexts

| Contexto | Servicio | Agregados | Responde a | Base |
|---|---|---|---|---|
| Identidad y acceso | Identity Service | Usuario, Rol, Autorización | Quién es el usuario y qué puede hacer | db_identity |
| Operación del parqueadero | Parking Service | Piso, Zona, Espacio, SesionParqueo | Qué espacios existen y qué vehículo ocupa cada uno | db_parking |
| Facturación y caja | Billing Service | Tarifa, Liquidacion, Pago, SesionCaja, Comprobante | Cuánto se cobra, cómo se paga y cómo se registra | db_billing |

## Relaciones entre contextos (mapa de contextos)

| Origen | Destino | Relación | Descripción |
|---|---|---|---|
| Parking | Billing | Cliente–Proveedor (Customer–Supplier) | Parking pide liquidaciones a Billing por REST con clave idempotente y espera un resultado terminal para cerrar. |
| Parking, Billing | Identity | Servicio de host abierto (Open Host Service) | Identity publica JWKS y un token estándar; los demás validan sin consultarla en cada petición. |
| Frontend | Los tres servicios | Conformista | El frontend consume los contratos REST tal como los publica cada servicio. |

Ver también `context_map.puml`.
