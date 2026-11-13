# Historias técnicas

| ID | Historia técnica | Estado |
|---|---|---|
| HT-01 | Versionar el esquema de cada servicio con Flyway | Implementada |
| HT-02 | Emitir JWT RS256 y publicar JWKS en Identity | Implementada |
| HT-03 | Idempotencia con Idempotency-Key en entradas, salidas y liquidaciones | Implementada |
| HT-04 | Cliente REST de Parking hacia Billing para crear o recuperar liquidaciones | Implementada |
| HT-05 | Sacar la llamada HTTP a Billing de la transacción y del bloqueo de sesión en solicitarSalida | Pendiente |
| HT-06 | Validar la migración Parking V5 en PostgreSQL temporal | Pendiente |
| HT-07 | Agregar healthchecks a las APIs en Docker Compose | Pendiente |
| HT-08 | Normalizar los prefijos de ruta de Parking (/api/v1 frente a /api/zonas y /api/sesiones) | Pendiente |
| HT-09 | Definir un rol técnico servicio a servicio para las rutas /internal/v1 de Billing | Pendiente |
| HT-10 | Ampliar pruebas de concurrencia, fallos de red y reintentos simultáneos | Pendiente |

## Criterios de aceptación

- **HT-01:** Dado que se despliega un servicio, cuando arranca, entonces aplica sus migraciones (Parking V1–V5, Identity V1–V3, Billing V1–V4) sobre su propia base.
- **HT-02:** Dado un JWT emitido por Identity, cuando Parking o Billing lo reciben, entonces validan la firma con la clave pública de /.well-known/jwks.json.
- **HT-03:** Dado una solicitud repetida con la misma clave, entonces se recupera el resultado original sin duplicar datos.
- **HT-04:** Dado una salida solicitada, entonces Parking obtiene la liquidación de Billing con la clave idempotente.
- **HT-05:** Dado una salida solicitada, entonces la llamada a Billing se ejecuta sin mantener abierta la transacción local ni el bloqueo de la sesión.
- **HT-06:** Dado una base temporal, cuando se ejecutan Parking V1–V5, entonces todas pasan y se actualiza la evidencia.
- **HT-07:** Dado el despliegue local, entonces cada servicio espera a que sus dependencias estén saludables, no solo iniciadas.
- **HT-08:** Dado el catálogo de endpoints de Parking, entonces todas las rutas siguen un mismo prefijo versionado.
- **HT-09:** Dado una llamada a /internal/v1, entonces solo la acepta un token de servicio y no uno de usuario operador.
- **HT-10:** Dado reintentos simultáneos de pago o cierre, entonces no se genera un segundo cobro ni un cierre duplicado.
