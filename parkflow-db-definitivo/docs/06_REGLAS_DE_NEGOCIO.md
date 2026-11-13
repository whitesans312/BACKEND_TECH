# 06 â€” Reglas de negocio

> Estado de las reglas tarifarias y pendientes comerciales: ver tambiÃ©n `../../PARKFLOW_IMPLEMENTATION_ROADMAP.md`. La configuraciÃ³n del recargo nocturno no autoriza por sÃ­ sola a aplicarlo automÃ¡ticamente.

## Confirmadas

- Horario operativo: 08:00â€“22:00 en `America/Bogota`; moneda COP.
- Gracia inicial de 15 minutos.
- Valores base: CARRO hora $6.000, dÃ­a $18.000, recargo nocturno $15.000; MOTO hora $3.500, dÃ­a $10.000, recargo $8.000.
- Tipos de vehÃ­culo: `CARRO` y `MOTO`; no hay clientes ni catÃ¡logo de vehÃ­culos laborales.
- Operador selecciona manualmente un espacio disponible, habilitado, sin mantenimiento y compatible.
- Un vehÃ­culo/espacio no puede tener mÃ¡s de una sesiÃ³n activa. OcupaciÃ³n se deriva de sesiones activas.
- Se exige efectivo y pago completo; no se permiten reembolsos. Pago aprobado, comprobante, movimiento y sesiÃ³n de caja cerrada son histÃ³ricos/inmutables.
- Cerrar caja no cierra ni libera sesiones de Parking.
- Solicitar liquidaciÃ³n no cierra Parking. Una cancelaciÃ³n mantiene sesiÃ³n y espacio activos.
- Cierre normal requiere confirmaciÃ³n Billing. Cierre excepcional requiere autorizaciÃ³n administrativa y evidencia, no un pago falso.
- Placa corregida y tiquete perdido dejan eventos trazables; cada ingreso crea una sesiÃ³n con snapshots y no se duplica ni conserva una entidad maestra de vehÃ­culo.
- El abandono es un procedimiento operativo separado de la salida normal y de la salida excepcional gratuita. Se documentan reporte, circunstancias, evidencias, contactos con autoridad, autorizaciÃ³n del cargo y retiro fÃ­sico.
- La sesiÃ³n sigue activa y mantiene ocupado el espacio durante la gestiÃ³n del abandono. Solo el retiro fÃ­sico permite cerrar la sesiÃ³n con motivo `ABANDONO` y liberar el espacio.
- Un cargo excepcional autorizado por abandono no se registra automÃ¡ticamente como efectivo ni como pago. Si se cobra, debe seguir una operaciÃ³n real de Billing con su integridad financiera habitual.

## CÃ¡lculo tarifario

PostgreSQL almacena valores, vigencias y snapshot; Java calcula. El desglose registra conceptos de hora, dÃ­a, recargo, gracia, ajuste/descuento si existe una regla aprobada. No hay CHECK de fÃ³rmula que fuerce una tarifa por minuto/hora lineal.

Los seis importes confirmados se modelan como filas de `tarifas` diferenciadas por tipo y concepto. Las fechas de vigencia y la conservaciÃ³n de snapshots permiten cambiar valores sin alterar liquidaciones anteriores. El cÃ¡lculo permanece en Java; la aplicaciÃ³n del recargo nocturno en estancias de varios dÃ­as sigue abierta.

**Regla confirmada:** los 15 minutos de gracia se descuentan antes de evaluar el umbral diario. Si el tiempo facturable supera 180 minutos, se cobra la tarifa diaria completa ($18.000 carro, $10.000 moto) en reemplazo de la tarifa por horas; no se suman. Para cada ciclo diario de permanencia se cobra el valor diario completo, sin prorratear ciclos parciales. El ciclo normal se reinicia a las 08:00. La combinaciÃ³n con recargos nocturnos sigue pendiente.

| Permanencia total | Tiempo tras gracia | Tarifa base |
|---|---:|---|
| 3 h 00 min | 2 h 45 min | Por horas |
| 3 h 15 min | 3 h 00 min | Por horas |
| 3 h 16 min | 3 h 01 min | Diaria: carro $18.000 / moto $10.000 |
| 4 h 00 min | 3 h 45 min | Diaria: carro $18.000 / moto $10.000 |

En una estancia que continÃºa durante mÃ¡s de un ciclo diario, se cobra la tarifa diaria completa por cada ciclo; un ciclo parcial no se prorratea.

**Regla confirmada:** los primeros 15 minutos desde el ingreso son gratuitos. DespuÃ©s, las horas se dividen por lÃ­mites de hora del reloj local: cualquier fracciÃ³n posterior a la gracia que cae dentro de un bloque horario cuenta como una hora completa. Ejemplo confirmado: ingreso 21:00; hasta 21:15 es gratis; salir a las 21:16 cuenta una hora; salir a las 22:10 cuenta dos horas (bloques 21:00â€“22:00 y 22:00â€“23:00). El eventual recargo nocturno se calcula aparte y sigue sujeto a las decisiones pendientes. La gracia no se repite por cada bloque horario.

## Estados propuestos

### SesiÃ³n de parqueo

```text
ACTIVA â”€â”€confirmaciÃ³n Billing pagadaâ”€â”€> CERRADA / PAGADA
ACTIVA â”€â”€confirmaciÃ³n Billing gratuitaâ”€> CERRADA / GRATUITA
ACTIVA â”€â”€autorizaciÃ³n Identity vÃ¡lidaâ”€â”€> CERRADA / EXCEPCIONAL
ACTIVA â”€â”€anulaciÃ³n autorizada vÃ¡lidaâ”€â”€> ANULADA
```

`CERRADA` y `ANULADA` son terminales. No hay FK de regreso desde sesiÃ³n a anulaciÃ³n/salida: los registros hijos referencian sesiÃ³n y un constraint trigger diferido valida evidencia al confirmar. No se libera espacio por cancelar una liquidaciÃ³n. La relaciÃ³n entre autorizaciÃ³n externa y sesiÃ³n se valida mediante operaciÃ³n REST idempotente mÃ¡s registro local.

La ruta de abandono tambiÃ©n es cierre terminal de sesiÃ³n: `ACTIVA` â†’ caso en gestiÃ³n â†’ retiro fÃ­sico confirmado â†’ `CERRADA/ABANDONO`. El diagrama anterior resume cierres comerciales/excepcionales; para abandono, la sesiÃ³n permanece activa durante la gestiÃ³n y solo cambia a cerrada despuÃ©s del retiro. `operacion_cierre_id` hace recuperable el reintento sin duplicar el cierre.

### LiquidaciÃ³n / pago

```text
LiquidaciÃ³n: PENDIENTE â”€â”€cliente/operador cancela solicitud antes del pagoâ”€â”€> CANCELADA
             PENDIENTE â”€â”€administrador corrige/anula con motivoâ”€â”€> ANULADA
             PENDIENTE â”€â”€monto cero (gracia)â”€â”€> GRATUITA
             PENDIENTE â”€â”€efectivo completo aprobadoâ”€â”€> PAGADA
Pago intento: PENDIENTE â”€â”€efectivo confirmadoâ”€â”€> APROBADO (terminal e inmutable)
              PENDIENTE â”€â”€fallo/rechazo antes de cobroâ”€â”€> RECHAZADO (terminal)
```

`CANCELADA` significa que el cliente decidiÃ³ no salir y se abandona esa solicitud; la sesiÃ³n y el espacio permanecen activos. `ANULADA` es correcciÃ³n administrativa documentada y solo aplica antes de un pago aprobado. `PAGADA` es terminal. `GRATUITA` es un resultado final de total cero, sin fila de pago ni movimiento de caja; no es `SIN_COSTO` como mÃ©todo de pago. Si habÃ­a intento pendiente, Billing debe cerrarlo/rechazarlo dentro del flujo antes de fijar el estado terminal.

### Caja

```text
ABIERTA â”€â”€conteo y cierreâ”€â”€> CERRADA (terminal)
```

La sesiÃ³n de caja se referencia desde el pago aprobado. Su cierre no depende de que el parqueadero estÃ© vacÃ­o. Los pagos nuevos usan una sesiÃ³n abierta diferente. Un pago aprobado, su movimiento y comprobante se confirman juntos.

### AutorizaciÃ³n excepcional

```text
EMITIDA â”€â”€consumo Ãºnico antes de expirarâ”€â”€> CONSUMIDA
EMITIDA â”€â”€revocaciÃ³n administrativaâ”€â”€â”€â”€â”€â”€â”€> REVOCADA
EMITIDA â”€â”€vencimientoâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€> EXPIRADA
```

La autorizaciÃ³n es Ãºnica para `jti + operacion_id + sesion_parking_id`. Identity registra el resultado de consumo: reintento con el mismo trÃ­o recupera Ã©xito anterior; JTI con otra operaciÃ³n o sesiÃ³n se rechaza. Una sesiÃ³n con pago aprobado no puede cerrarse excepcionalmente; antes de consumir el JTI se debe resolver/cancelar idempotentemente una liquidaciÃ³n pendiente en Billing. La autorizaciÃ³n no habilita cerrar una sesiÃ³n distinta.

### Abandono de vehÃ­culo

```text
ABANDONO_REPORTADO â†’ EN_GESTION â†’ AUTORIDAD_CONTACTADA
                   â†’ RETIRO_COORDINADO â†’ VEHICULO_RETIRADO â†’ CERRADO
```

El historial guarda quiÃ©n reportÃ³, cuÃ¡ndo, motivo y evidencias; contactos con autoridad; monto excepcional autorizado y quiÃ©n lo autorizÃ³; y hora/responsable del retiro. El caso y la eventual deuda son independientes de la caja del operador. No se genera pago ficticio ni se libera el espacio antes del retiro. La forma de cobro al propietario o tratamiento de saldo no recuperado sigue pendiente.

La autorizaciÃ³n del cargo es un propÃ³sito Identity separado de autorizar una salida excepcional gratuita. Cargo autorizado, eventual cuenta por cobrar y pago recibido son conceptos distintos; no se crea cuenta por cobrar automÃ¡ticamente mientras la polÃ­tica siga pendiente.

En Billing, `LIQUIDADO` significa que se creÃ³ una liquidaciÃ³n vinculada al cargo; no acredita recepciÃ³n de dinero. `PAGADO` solo se alcanza al aprobar un pago real con su movimiento de caja y comprobante. Una liquidaciÃ³n originada en cargo de abandono no puede ser `GRATUITA`. Si el cargo se revoca antes de liquidar, se conserva como histÃ³rico; la propuesta admite autorizaciones/cargos sucesivos, con mÃ¡ximo uno vigente por caso, sujeto a confirmar la polÃ­tica de revocaciÃ³n y reintento.

## VehÃ­culos y privacidad operativa

No se mantiene tabla maestra `vehiculos`. `sesiones_parqueo` guarda `placa_ingreso`, `placa_actual` y `tipo_vehiculo`; una correcciÃ³n modifica la placa activa y aÃ±ade evento con anterior/nueva. La bÃºsqueda por placa indexa solo sesiones activas. Al cierre no queda un catÃ¡logo reutilizable para reconocer vehÃ­culos de trabajadores; el historial conserva snapshot por el plazo operativo/contable aprobado. DespuÃ©s se recomienda anonimizar placa y valores relacionados en eventos de acuerdo con la polÃ­tica legal, manteniendo mÃ©tricas y trazabilidad no identificable. El plazo exacto y cualquier obligaciÃ³n de retener comprobantes con placa estÃ¡n pendientes.

## Personas con ambos roles

Cada acciÃ³n valida el permiso concreto (`OPERADOR` al solicitar, `ADMINISTRADOR` al autorizar), aunque el mismo usuario tenga ambos roles. Se guardan actor y acciÃ³n en filas/eventos separados y se snapshotizan los roles evaluados. No se deduce autorizaciÃ³n de un rol global del frontend. Si se exige que quien autoriza sea una persona distinta al solicitante, debe aprobarse como regla de separaciÃ³n de funciones.

