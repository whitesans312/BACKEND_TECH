# 07 — Decisiones pendientes

**Actualizado:** 2026-11-12. Las migraciones activas (Parking V1–V4, Identity V1–V3 y Billing V1–V4) se validaron en PostgreSQL 16.15 temporal. La situación de código y los alcances no implementados están en [`PARKFLOW_IMPLEMENTATION_ROADMAP.md`](../../PARKFLOW_IMPLEMENTATION_ROADMAP.md). Esta lista contiene decisiones comerciales o de operación aún no resueltas; no bloquean los flujos ya implementados.

## Decisiones comerciales que requieren al propietario

### Recargo nocturno y horario

El recargo está configurado en $15.000 para carro y $8.000 para moto, pero no se debe aplicar automáticamente hasta definir:

- Si se cobra una vez por salida, por cada noche o por cada cruce de las 22:00.
- Qué sucede cuando el ingreso ocurre después de las 22:00, antes de las 08:00 o durante el horario cerrado.
- Si se reciben vehículos entre 22:00 y 08:00 o si 08:00–22:00 limita ingresos.

**Recomendación:** parametrizar el intervalo y el método de cobro, pero mantener el recargo deshabilitado hasta confirmar estos puntos.

### Abandono: cargo y entrega

No está definido el importe del cargo excepcional ni el tratamiento cuando el propietario nunca reclama el vehículo. Tampoco está decidido si pagar ese cargo es condición para entregarlo. Por ello el sistema no debe crear automáticamente deuda, liquidación ni pago al reportar abandono.

**Decisión requerida:** confirmar si el pago del cargo será obligatorio antes de la entrega y cómo se fija el importe (tabla tarifaria aprobada o autorización administrativa por caso). No se debe atribuir una respuesta al propietario sin su confirmación.

### Autorizaciones administrativas

La prohibición de que un usuario se autorice a sí mismo no está resuelta. Tampoco se han fijado vencimiento, identidad de servicio/claims y procedimiento para retirar o revocar autorización sin perder auditoría.

**Recomendación:** exigir dos personas distintas para solicitar y aprobar, usar vigencia corta y consumo idempotente de un solo uso. Requiere aprobación del propietario antes de habilitar esas salidas.

### Caja y conservación de datos

- Definir si un operador puede tener varias sesiones de caja abiertas y si una caja puede tener varias sesiones simultáneas.
- Definir si se registran egresos, retiros o ajustes aparte de pagos de parqueo y qué autorizaciones exigen.
- Definir si el cierre requiere denominaciones del efectivo o solo un valor total contado.
- Definir plazo de retención/anonimización de placas y excepciones para registros contables/legales.

**Recomendación:** no ampliar los tipos de movimiento ni anonimizar registros hasta fijar política de auditoría y retención.

## Comportamientos implementados que conviene ratificar

- La liquidación gratuita total cero genera constancia operativa, sin pago ni movimiento de caja. La validez tributaria no se infiere; si se requiere comprobante fiscal, debe definirse aparte.
- Una caja se puede cerrar aunque haya vehículos estacionados; el estacionamiento no depende de la sesión de caja del operador.
- Una liquidación pendiente puede cancelarse sin cerrar la sesión ni liberar el espacio. La aplicación de esa cancelación está en Billing; Parking conserva la sesión activa.

## Decisiones ya confirmadas

- Se descuenta una gracia inicial de 15 minutos.
- El umbral de 180 minutos se mide después de descontar la gracia; solo cuando el tiempo facturable **supera** 180 minutos se cobra tarifa diaria en reemplazo de las horas.
- Para hasta 180 minutos facturables se cobran bloques de reloj tocados por hora o fracción completa.
- La tarifa diaria por ciclo es completa y no se prorratea: carro $18.000, moto $10.000. Ciclo desde las 08:00 en `America/Bogota`.
- Los pagos son únicamente en efectivo, no hay pagos parciales, y pagos aprobados no se modifican ni devuelven.
- No hay clientes ni catálogo permanente de vehículos; la asignación de espacio es manual.
- Una sesión puede seguir activa con una caja cerrada. La salida excepcional exige autorización del administrador.
- Solicitud de liquidación solo cuando el cliente solicita salir. Para tiquete perdido se localiza por placa y se exige tarjeta de propiedad según el proceso operativo.

## Pendientes técnicos (no son decisiones comerciales)

- Traslados, mantenimiento, anulaciones, autorizaciones de salida excepcional y expediente de abandono/retiro deben implementarse antes de afirmar cobertura completa. Ver el roadmap.
- Extender pruebas para concurrencia real, reintentos simultáneos, fallos de red y reconciliación. Testcontainers no se ejecutó en el entorno actual al no disponer de Docker.
- Determinar la necesidad de API Gateway, observabilidad, copias/restauración y política de despliegue antes de producción.
