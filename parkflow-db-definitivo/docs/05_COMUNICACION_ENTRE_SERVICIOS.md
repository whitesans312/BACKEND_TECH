# 05 â€” ComunicaciÃ³n entre servicios

> Contratos presentes y pendientes: las APIs implementadas cubren autenticaciÃ³n, operaciÃ³n de estacionamiento, liquidaciÃ³n/caja/pago y cierre normal. Las APIs de autorizaciÃ³n excepcional y abandono siguen pendientes, segÃºn el roadmap de implementaciÃ³n.

No existen FK ni transacciones distribuidas. Cada endpoint interno autentica al servicio llamador, autoriza el actor a partir de JWT validado y acepta clave de idempotencia en operaciones mutables. Nunca se confÃ­a en un `operador_id` arbitrario enviado por el navegador.

## Ingreso

1. Frontend solicita a Parking registrar placa/tipo y espacio seleccionado.
2. Parking valida permiso del operador, compatibilidad, disponibilidad y estado del espacio; crea sesiÃ³n activa con snapshot de placa/tipo y tiquete en una transacciÃ³n local. No crea un registro maestro de vehÃ­culo.
3. La unicidad parcial de sesiÃ³n activa evita una segunda ocupaciÃ³n aunque existan dos solicitudes concurrentes. Respuesta incluye `sesion_id`, `numero_tiquete` y hora.

## Salida normal y cobro

1. Operador presenta tiquete o placa; en caso de tiquete perdido Parking registra el evento y exige la verificaciÃ³n operativa definida antes de continuar.
2. Parking consulta sesiÃ³n activa y solicita a Billing crear liquidaciÃ³n con `sesion_parqueo_id`, snapshot de entrada/vehÃ­culo, corte, operador y clave idempotente. Billing lee su tarifa vigente y calcula/desglosa en Java; persiste snapshot y detalles sin cerrar la sesiÃ³n.
3. El operador recibe total y confirma efectivo. Billing serializa por liquidaciÃ³n, confirma caja abierta, marca liquidaciÃ³n pagada y pago aprobado, aÃ±ade movimiento y comprobante en **una transacciÃ³n local**. Los importes deben cuadrar exactamente.
4. Parking consulta/recibe el resultado Billing por el ID idempotente. Solo entonces cierra sesiÃ³n como pagada y libera el espacio en transacciÃ³n Parking.
5. Si la respuesta de red se pierde, Parking no cierra con resultado incierto. Consulta el estado por clave de idempotencia y reintenta sin crear segundo cobro.

No hay transacciÃ³n atÃ³mica entre Billing y Parking: puede existir pago aprobado mientras Parking siga activa por una caÃ­da. La operaciÃ³n de consulta/reintento/reconciliaciÃ³n debe completar el cierre sin volver a cobrar. No se debe compensar con devoluciÃ³n, porque no estÃ¡ permitida.

## LiquidaciÃ³n cancelada y salida gratuita

- Cancelar una liquidaciÃ³n pendiente en Billing no llama al cierre de Parking; la sesiÃ³n y el espacio siguen activos. El intento/estado queda en historial. Nueva solicitud usa otra clave idempotente.
- Para total cero dentro de gracia no se crea pago de valor cero ni movimiento de caja. Billing cambia la liquidaciÃ³n a `GRATUITA` y devuelve confirmaciÃ³n idempotente. Parking cierra con `tipo_cierre=GRATUITA`, separado de `PAGADA` y `EXCEPCIONAL`.
- El modelo admite una constancia gratuita opcional en `comprobantes` (`tipo=SALIDA_GRATUITA`, sin pago, asociada una a una a la liquidaciÃ³n y con total cero). AÃºn requiere confirmar si la operaciÃ³n la necesita; nunca se llama comprobante de pago.

## Salida excepcional

1. Parking registra solicitud local ligada a sesiÃ³n activa y motivo. Genera y persiste un `operacion_id` UUID estable antes de llamar a Identity.
2. Identity valida que el solicitante tenga permiso `OPERADOR` y el aprobador `ADMINISTRADOR`; registra ambas acciones y los roles/permisos comprobados. Si un usuario tiene ambos roles, cada endpoint vuelve a validar el permiso correspondiente y conserva los dos eventos. Si se permite autoaprobaciÃ³n queda sujeto a decisiÃ³n del propietario.
3. Identity emite autorizaciÃ³n con `jti` Ãºnico, sesiÃ³n, solicitud y `operacion_id`; persiste esos vÃ­nculos. Parking verifica firma, issuer, audience y expiraciÃ³n.
4. Antes de consumir, Parking llama a Billing con el mismo `operacion_id` para resolver el estado financiero: si hay pago aprobado, Identity revoca la autorizaciÃ³n aÃºn no consumida y Parking recupera el cierre normal; si hay liquidaciÃ³n o pago pendiente, Billing los cancela/rechaza idempotentemente bajo bloqueo de filas; si no hay pendientes, confirma que no hay cobro aprobado.
5. Parking serializa operaciones de salida mientras la solicitud excepcional siga activa. Las solicitudes de liquidaciÃ³n pasan por Parking y se rechazan/enfilan durante ese cierre; se impide iniciar otra liquidaciÃ³n entre la cancelaciÃ³n Billing y el cierre excepcional. El bloqueo se liga al `operacion_id` para reintentar la misma operaciÃ³n.
6. Parking solicita `consumir(jti,operacion_id,sesion_parking_id)`. En una transacciÃ³n local Identity cambia `EMITIDA â†’ CONSUMIDA` guardando operaciÃ³n/sesiÃ³n y resultado. Si Identity ya consumiÃ³ ese JTI para **el mismo** `operacion_id` y sesiÃ³n, devuelve el resultado guardado como Ã©xito idempotente. Si operaciÃ³n o sesiÃ³n difieren, devuelve conflicto; nunca reasocia el JTI.
7. Tras obtener/recuperar el resultado, Parking inicia una transacciÃ³n local, bloquea la sesiÃ³n y verifica que siga activa, sin pago aprobado ni liquidaciÃ³n pendiente. Inserta la salida excepcional y cierra sesiÃ³n; un constraint trigger diferido exige evidencia hija antes del COMMIT. `UNIQUE(jti)` y `UNIQUE(operacion_id)` impiden reutilizar autorizaciÃ³n.
8. Si Identity consume y Parking pierde conexiÃ³n antes de cerrar, reintenta con los mismos tres identificadores. Identity devuelve el consumo anterior y Parking completa la misma operaciÃ³n. No hay una transacciÃ³n distribuida; el `operacion_id` persistido permite recuperaciÃ³n exacta, y un reintento diferente se rechaza.

## Contratos implementados y faltantes

Implementados en esta versiÃ³n: `POST /internal/v1/liquidaciones`, lectura por clave/ID, `POST /internal/v1/pagos/{id}/resolver`, APIs de cajas y el cierre coordinado de Parking. Los endpoints de excepciÃ³n/abandono siguen pendientes:

| Llamada | DueÃ±o | Claves/resultado |
|---|---|---|
| Crear/consultar/cancelar liquidaciÃ³n | Billing | `Idempotency-Key`; estados `PENDIENTE/CANCELADA/PAGADA/ANULADA/GRATUITA`; total, moneda, snapshot y detalle. |
| Confirmar pago en efectivo | Billing | `Idempotency-Key` de pago; `liquidacion_id`, `sesion_caja_id`; recibo y pago persistidos al confirmar. |
| Consultar resultado de salida cobrada | Billing | clave de liquidaciÃ³n/pago; lectura idempotente para resolver timeout. |
| Solicitar/autorizar/consumir salida excepcional | Identity | ID de solicitud, sesiÃ³n, `jti`, expiraciÃ³n, actor, `operacion_id`; consumo idempotente por el par operaciÃ³n/sesiÃ³n y rechazo de otro par. |
| Cerrar sesiÃ³n | Parking | requiere confirmaciÃ³n durable Billing o autorizaciÃ³n Identity vÃ¡lida; no admite datos de actor no autenticados. |
| Autorizar cargo de abandono | Identity â†’ Billing | PropÃ³sito/permisos propios, `caso_abandono_id`, importe COP, motivo, solicitante, administrador y clave idempotente; Billing valida correspondencia de importe con autorizaciÃ³n y persiste el cargo, nunca el pago. |
| Liquidar/cobrar cargo | Billing | Solo tras polÃ­tica comercial definida; `cargo_abandono_id` vincula una liquidaciÃ³n real. Pago aprobado usa caja/movimiento/comprobante atÃ³micos como cualquier cobro real. |

## Caso de abandono

1. Parking registra el reporte y sus circunstancias; la sesiÃ³n permanece activa y el espacio ocupado.
2. Parking documenta evidencias y cada contacto con la autoridad. El contacto se registra operativamente; no se presupone una integraciÃ³n automÃ¡tica con la PolicÃ­a.
3. Identity tramita un permiso especÃ­fico `AUTORIZAR_CARGO_ABANDONO`, distinto de la autorizaciÃ³n de salida excepcional gratuita; registra solicitante, administrador, importe/motivo autorizados y auditorÃ­a. Billing verifica la autorizaciÃ³n y persiste un cargo con los ID externos del caso y autorizaciÃ³n. Esta operaciÃ³n no crea deuda exigible automÃ¡ticamente, pago, comprobante ni movimiento de caja.
4. Se documentan coordinaciÃ³n y retiro efectivo, incluida la persona o autoridad que recibe el vehÃ­culo cuando corresponda.
5. Solo despuÃ©s del retiro fÃ­sico Parking cierra la sesiÃ³n con `tipo_cierre=ABANDONO` y estado `CERRADA`, registrando retiro/caso y liberando el espacio en una transacciÃ³n local. La misma clave `operacion_cierre_id` permite reintentar sin duplicar el cierre. La cobranza y cualquier cuenta por cobrar siguen pendientes de decisiÃ³n comercial.

Una liquidaciÃ³n de ese cargo solo se crea cuando la polÃ­tica de cobro sea aprobada y se produzca el evento definido por el negocio. El pago real conserva el flujo estÃ¡ndar Billing: aprobaciÃ³n, movimiento de caja y comprobante en una transacciÃ³n.

## ReconciliaciÃ³n

Procesos de solo lectura reportan: pago aprobado sin comprobante/movimiento; movimiento sin pago aprobado o con monto/caja/liquidaciÃ³n diferente; comprobante que no corresponde al monto/liquidaciÃ³n del pago; liquidaciÃ³n pagada sin pago aprobado; sesiÃ³n Parking cerrada como pagada cuyo resultado Billing no es pagado; sesiÃ³n activa con pago aprobado; salida excepcional sin autorizaciÃ³n consumida o con distinta operaciÃ³n/sesiÃ³n; JTI consumido sin cierre tras el periodo de recuperaciÃ³n. La reparaciÃ³n requiere reintento de la misma operaciÃ³n y registro de auditorÃ­a; nunca editar pagos histÃ³ricos directamente.

