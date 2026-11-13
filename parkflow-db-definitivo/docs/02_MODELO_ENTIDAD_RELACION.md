# 02 â€” Modelo entidad-relaciÃ³n

> Este modelo define el objetivo de datos. Su implementaciÃ³n vigente estÃ¡ en las migraciones Flyway de `../parking`, `../identity` y `../billing`; el alcance funcional que todavÃ­a falta se registra en `../../PARKFLOW_IMPLEMENTATION_ROADMAP.md`.

Los diagramas muestran relaciones **dentro** de cada base. Los UUID externos (operadores, administradores, sesiones y liquidaciones de otros servicios) se validan por contrato REST, no con claves forÃ¡neas entre bases.

## Parking â€” `db_parking`

```mermaid
erDiagram
  PISOS ||--o{ ZONAS : contiene
  ZONAS ||--o{ ESPACIOS : contiene
  ESPACIOS ||--o{ SESIONES_PARQUEO : asignado_en
  SESIONES_PARQUEO o|--o{ EVENTOS_OPERATIVOS : registra
  ESPACIOS ||--o{ MANTENIMIENTOS_ESPACIO : recibe
  SESIONES_PARQUEO ||--o{ TRASLADOS_ESPACIO : se_traslada
  ESPACIOS ||--o{ TRASLADOS_ESPACIO : origen_o_destino
  SESIONES_PARQUEO ||--o{ SOLICITUDES_SALIDA_EXCEPCIONAL : solicita
  SOLICITUDES_SALIDA_EXCEPCIONAL ||--o| SALIDAS_EXCEPCIONALES : ejecutada_como
  SESIONES_PARQUEO ||--o| SALIDAS_EXCEPCIONALES : evidencia
  SESIONES_PARQUEO ||--o| ANULACIONES_SESION : evidencia
  SESIONES_PARQUEO ||--o| CASOS_ABANDONO : reportado
  CASOS_ABANDONO ||--o{ CONTACTOS_AUTORIDAD : documenta
  CASOS_ABANDONO ||--o{ EVIDENCIAS_ABANDONO : conserva
  CASOS_ABANDONO ||--o| RETIROS_VEHICULO : registra
```

No existe catÃ¡logo persistente `vehiculos`: placa y tipo se guardan en la sesiÃ³n como snapshot. La bÃºsqueda por placa solo indexa sesiones activas. El tiquete es un atributo Ãºnico e inmutable de la sesiÃ³n. AnulaciÃ³n y salida excepcional son tablas hijas que referencian sesiÃ³n; `sesiones_parqueo` no apunta de regreso a ellas, asÃ­ se elimina el ciclo FK. Un constraint trigger diferido o transacciÃ³n de servicio exige una fila hija coincidente al confirmar una sesiÃ³n cerrada/anulada. La salida pagada guarda `liquidacion_id` de Billing como UUID externo; la gratuita es una liquidaciÃ³n de total cero sin fila en `pagos`.

## Identity â€” `db_identity`

```mermaid
erDiagram
  USUARIOS ||--o{ USUARIO_ROLES : recibe
  ROLES ||--o{ USUARIO_ROLES : asignado
  USUARIOS ||--o{ AUTORIZACIONES_EXCEPCIONALES : solicita
  USUARIOS ||--o{ AUTORIZACIONES_EXCEPCIONALES : autoriza
  USUARIOS ||--o{ AUTORIZACIONES_CARGO_ABANDONO : solicita_o_autoriza
  USUARIOS o|--o{ AUDITORIA_SEGURIDAD : actor
```

`solicitud_parking_id` y `sesion_parking_id` son UUID externos. El JTI identifica la autorizaciÃ³n firmada y tiene unicidad local; no se guarda el JWT completo. `operacion_id` identifica el intento de cierre creado por Parking. El consumo conserva operaciÃ³n y sesiÃ³n; solo el mismo par puede repetirse idempotentemente.

## Billing â€” `db_billing`

```mermaid
erDiagram
  LIQUIDACIONES ||--o{ DETALLES_LIQUIDACION : desglosa
  TARIFAS ||--o{ DETALLES_LIQUIDACION : referencia
  LIQUIDACIONES ||--o{ PAGOS : recibe_intentos
  CAJAS ||--o{ SESIONES_CAJA : abre
  SESIONES_CAJA o|--o{ PAGOS : recibe
  SESIONES_CAJA ||--o{ MOVIMIENTOS_CAJA : registra
  PAGOS o|--o| MOVIMIENTOS_CAJA : genera
  PAGOS o|--o| COMPROBANTES : acredita
  LIQUIDACIONES ||--o| COMPROBANTES : constancia_o_recibo
  LIQUIDACIONES ||--o| ANULACIONES_LIQUIDACION : anulacion_administrativa
  CARGOS_ABANDONO ||--o{ EVENTOS_CARGO_ABANDONO : historial
  CARGOS_ABANDONO ||--o| LIQUIDACIONES : puede_cobrarse_en
```

`liquidaciones.sesion_parqueo_id` es UUID externo de Parking. Las lÃ­neas guardan el snapshot del cÃ¡lculo. `comprobantes.pago_id` es nullable Ãºnicamente para una posible constancia gratuita; `liquidacion_id` identifica siempre la operaciÃ³n. FKs compuestas con monto impiden que comprobante o movimiento difieran del pago, su liquidaciÃ³n o su sesiÃ³n de caja. Constraint triggers diferidos exigen comprobante y movimiento al confirmar un pago aprobado.

El caso de abandono y su evidencia operativa pertenecen a Parking. Identity conserva la autorizaciÃ³n administrativa del cargo con propÃ³sito propio; Billing conserva el cargo autorizado, su historial y, si ocurre, la liquidaciÃ³n/pago real. El UUID del caso es referencia externa y no crea FK entre bases. Autorizar un cargo no crea automÃ¡ticamente saldo por cobrar, pago, movimiento de caja ni comprobante.

