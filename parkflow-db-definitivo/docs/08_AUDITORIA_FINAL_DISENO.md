# 08 â€” AuditorÃ­a final del diseÃ±o

> **AuditorÃ­a documental histÃ³rica.** El alcance y dictamen siguientes corresponden al estado anterior a la implementaciÃ³n integral. Desde entonces se generaron las migraciones activas y se adaptaron los tres servicios; la validaciÃ³n actual y el estado funcional se documentan en `../../PARKFLOW_IMPLEMENTATION_ROADMAP.md`.

## Alcance y dictamen

Se revisaron los siete documentos de `parkflow-db-definitivo/docs/` y la estructura/configuraciÃ³n visible de `services/parking-service`, `services/identity-service` y `services/billing-service`. No se ejecutÃ³ SQL, no se conectÃ³ a PostgreSQL y no se modificÃ³ el backend. La auditorÃ­a es documental y de lectura; no equivale a validar las migraciones existentes ni su comportamiento real.

**Dictamen: NO APROBADO para generar las migraciones definitivas.** Hay contradicciones de modelo y faltan decisiones comerciales que afectan tablas, estados y transacciones. El diseÃ±o puede continuar a implementaciÃ³n tras resolver los hallazgos crÃ­ticos/altos y actualizar los documentos relacionados.

## Hallazgos crÃ­ticos

| ID | Hallazgo | Evidencia / impacto | CorrecciÃ³n recomendada |
|---|---|---|---|
| C-1 | El flujo de abandono estÃ¡ descrito, pero su modelo financiero no existe. | `02` afirma que Billing guarda autorizaciÃ³n/cargo excepcional; `05` prescribe registrarlo allÃ­; `03` solo aÃ±ade `cargo_billing_id` externo al caso, sin tabla de cargos en Billing. No pueden persistirse estado de deuda, monto autorizado ni recuperaciÃ³n. | Definir en Billing una entidad de cargo autorizado vinculada por UUID externo al caso Parking, con actor, importe, estado e historial. Acordar estados antes de migrar. Pago efectivo solo cuando realmente se cobre. |
| C-2 | El cierre de sesiÃ³n por abandono no estÃ¡ integrado de forma consistente en el modelo de sesiÃ³n. | `03` deja `tipo_cierre` limitado a `PAGADA/GRATUITA/EXCEPCIONAL`, aunque luego describe `ABANDONO`; `02` dibuja el caso hijo, pero el texto solo menciona evidencia para anulaciÃ³n/salida excepcional; `04` exige un caso retirado, pero no define la transiciÃ³n completa de sesiÃ³n activa a cerrada. | Incluir `ABANDONO` en el catÃ¡logo de cierre y especificar una Ãºnica regla de transiciÃ³n/validaciÃ³n: caso registrado, retiro fÃ­sico persistido, hora/operador de cierre y liberaciÃ³n del espacio en la misma transacciÃ³n local. |
| C-3 | Las restricciones referidas a otras filas/servicios no se pueden implementar como `CHECK`. | `04` mezcla CHECK/trigger para reglas que consultan pagos, caja, casos hijos y estados en otras tablas. PostgreSQL CHECK no garantiza invariantes que dependan de otras filas/tablas. Una FK tampoco puede cruzar bases. | Reservar CHECK para la fila actual. Usar FK/UNIQUE locales, triggers apropiados o transacciones de servicio con bloqueos y una ruta Ãºnica de escritura. Las comprobaciones entre servicios deben ser contratos/reconciliaciÃ³n, nunca restricciones PostgreSQL locales. Documentar el mecanismo por regla. |
| C-4 | El tratamiento de un cargo de abandono puede contradecir el modelo actual de liquidaciÃ³n/pago. | El negocio aÃºn no decide si se cobra al reclamar, queda como deuda aunque el dueÃ±o no aparezca, si impide la entrega ni cÃ³mo se determina/perdona. No hay cliente/propietario persistente y el flujo actual de `liquidaciones` estÃ¡ ligado a una sesiÃ³n. | Resolver las decisiones de la secciÃ³n â€œDecisiones pendientesâ€. DiseÃ±ar el cargo independiente antes de asociarlo a una liquidaciÃ³n normal; no inferir identidad del propietario ni condicionar la entrega del vehÃ­culo sin autorizaciÃ³n. |

## Inconsistencias entre documentos

| Prioridad | Inconsistencia | Documento(s) | Ajuste |
|---|---|---|---|
| ALTO | Propiedad de datos dice â€œvehÃ­culos operativosâ€ en Parking, lo cual parece contradecir que no existe catÃ¡logo persistente; el documento de arquitectura tambiÃ©n incluye â€œmovimientosâ€ sin distinguir movimientos de espacios y de caja. | `01`, `03`, `06` | Cambiar el lÃ­mite de Parking a sesiones/snapshots operativos, y nombrar explÃ­citamente movimientos de espacio en Parking y movimientos de caja en Billing. |
| ALTO | `02` diagrama `ANULACIONES_SESION` como hija directa, pero la descripciÃ³n y `03` la tratan como evidencia hija; el control de fila hija se formula genÃ©ricamente como cierre/anulaciÃ³n, sin enumerar casos de cierre normales y abandono. | `02`, `03`, `04`, `06` | Explicitar por tipo de cierre quÃ© evidencia local es obligatoria. El cierre pagado/gratuito requiere confirmaciÃ³n Billing; excepcional requiere salida hija y autorizaciÃ³n; abandono requiere caso y retiro. |
| ALTO | `06` incluye abandono en texto, pero su diagrama de estados de sesiÃ³n omite la ruta `ABANDONO`; ademÃ¡s el estado `CERRADO` del expediente no estÃ¡ definido con claridad respecto del cierre de la sesiÃ³n. | `03`, `06` | Definir si el caso pasa a `CERRADO` dentro de la misma transacciÃ³n que el cierre Parking o despuÃ©s, con transiciÃ³n recuperable/idempotente. |
| ALTO | `05` dice que Identity autoriza la tarifa excepcional de abandono, pero `03` no agrega ese propÃ³sito/relaciÃ³n a Identity; la tabla existente de Identity es para salida excepcional. | `03`, `05`, `07` | Especificar un permiso/registro de autorizaciÃ³n para el cargo de abandono (o justificar reutilizaciÃ³n de autorizaciÃ³n genÃ©rica), con propÃ³sito distinto de salida excepcional gratuita. |
| MEDIO | El orden de numeraciÃ³n de pendientes en `07` salta de 17 a 22â€“23 y luego vuelve a 18â€“21. | `07` | Renumerar secuencialmente y organizar por tema antes de aprobar el diseÃ±o. |
| MEDIO | `07` aÃºn pregunta si se aprueba salida gratuita y varias polÃ­ticas operativas, por lo que la lista de reglas confirmadas no equivale a aprobaciÃ³n de todo el modelo. | `06`, `07` | Mantener el dictamen de propuesta hasta que cada decisiÃ³n que afecte el esquema tenga respuesta explÃ­cita. |

## RevisiÃ³n por servicio

### Parking

- Responsabilidad adecuada: infraestructura, espacios, sesiones, tiquetes, eventos y expediente de abandono.
- No debe existir catÃ¡logo permanente de vehÃ­culos ni FK a Identity/Billing.
- Falta reflejar `tipo_cierre=ABANDONO` y una validaciÃ³n completa de cierre/retiro en la fila de sesiÃ³n.
- La sesiÃ³n activa debe seguir ocupando el espacio hasta el retiro fÃ­sico, conforme a `04/06`; esta regla todavÃ­a necesita conectarse con todas las rutas de cierre.

### Identity

- Responsabilidad adecuada: usuarios, roles, credenciales, permisos y auditorÃ­a.
- La autorizaciÃ³n descrita en el modelo corresponde principalmente a salidas excepcionales. No estÃ¡ definido el registro/autorizaciÃ³n para monto excepcional por abandono.
- La regla de autoaprobaciÃ³n sigue pendiente y debe permanecer asÃ­ en tablas/polÃ­ticas hasta respuesta.

### Billing

- Responsabilidad adecuada: tarifas, liquidaciones, pagos en efectivo, cajas, movimientos y comprobantes.
- Faltan entidades para cargo excepcional autorizado y su ciclo de recuperaciÃ³n; un cargo no puede confundirse con pago aprobado.
- Pagos aprobados deben continuar ligados a importe exacto, caja abierta, movimiento y comprobante en una sola transacciÃ³n local. La documentaciÃ³n describe el objetivo, pero no se ha ejecutado una validaciÃ³n PostgreSQL.

La estructura visible confirma tres proyectos Java 21/Spring Boot 3.5.14 con PostgreSQL JDBC y Flyway; los perfiles normales declaran Hibernate `ddl-auto=validate` y Flyway desactivado (con variaciÃ³n configurable en Identity). No se encontrÃ³ una versiÃ³n objetivo de PostgreSQL ni una configuraciÃ³n de infraestructura denominada `infraestructura` en la ruta revisada. La compatibilidad de versiÃ³n no puede certificarse hasta fijar versiÃ³n mÃ­nima/objetivo y probar las migraciones contra ella.

## Viabilidad tÃ©cnica PostgreSQL

| Mecanismo | EvaluaciÃ³n / condiciÃ³n |
|---|---|
| FK compuestas | Viables dentro de una base. La combinaciÃ³n referenciada debe ser PK o tener UNIQUE compatible, con tipos y orden de columnas coincidentes. No vinculan distintas bases. |
| CHECK | Adecuados para dominio y consistencia de columnas de una fila, por ejemplo importe no negativo, moneda COP o estado permitido. No deben consultar tablas ni invocar funciones que dependan de datos cambiantes externos. |
| Ãndices Ãºnicos parciales | Viables para invariantes como una sesiÃ³n activa por placa/espacio; su predicado debe ser estable y coincidir con las consultas de escritura. |
| Constraint triggers diferidos | PostgreSQL los soporta como constraint triggers `AFTER ROW`, `DEFERRABLE`, con comprobaciÃ³n al final de la transacciÃ³n. Deben comprobar filas locales y todos los caminos de escritura; no sustituyen bloqueos/Ã­ndices contra concurrencia ni consultan otra base. |
| Idempotencia | Unicidad de clave + persistencia del resultado en la misma transacciÃ³n que el efecto local. Para operaciones REST distribuidas se requieren claves estables, consulta/reintento y reconciliaciÃ³n. |
| Concurrencia | La combinaciÃ³n de bloqueo de filas, orden consistente de bloqueos, transacciones acotadas e Ã­ndices Ãºnicos puede serializar decisiones locales. La documentaciÃ³n debe indicar los recursos bloqueados en cada operaciÃ³n para evitar carreras y deadlocks. |
| Inmutabilidad | Permisos SQL mÃ­nimos mÃ¡s triggers o procedimientos para prohibir UPDATE/DELETE en filas financieras y auditorÃ­a. Debe definirse quÃ© roles privilegiados pueden corregir datos y cÃ³mo se registra esa acciÃ³n. |
| Roles | La separaciÃ³n app/migrator estÃ¡ bien planteada conceptualmente. Falta inventario final de privilegios por tabla, secuencia y funciÃ³n, dueÃ±o de objetos, `search_path` y `ALTER DEFAULT PRIVILEGES`. |

## Reglas confirmadas y pendientes

**Confirmadas en `06`:** horario base 08:00â€“22:00 (zona `America/Bogota`), 15 minutos iniciales de gracia, cobro en efectivo y pago completo, CARRO/MOTO y valores base; tras descontar gracia, cuando se superan 180 minutos la tarifa diaria reemplaza la horaria; se cobra tarifa diaria completa por ciclo, sin prorratear; bloques horarios se cuentan por lÃ­mites del reloj segÃºn ejemplo; asignaciÃ³n manual; no hay registro de clientes ni catÃ¡logo permanente de vehÃ­culos; cancelar liquidaciÃ³n no cierra sesiÃ³n; cerrar caja no libera vehÃ­culos; abandono requiere retiro fÃ­sico para cierre y no produce pago automÃ¡tico.

**Pendientes comerciales/operativos registrados:** recargo nocturno (frecuencia, cruces y entradas fuera de horario); reinicio de gracia a las 08:00 durante estancias largas; admisiÃ³n fuera de horario; flujo/constancia y caja para salida gratuita; lÃ­mites de intentos rechazados; cancelaciÃ³n y recuperaciÃ³n distribuida; retenciÃ³n de placas; cardinalidad de cajas abiertas; otros movimientos de caja; saldo inicial/denominaciones; duraciÃ³n/claims de autorizaciones y autoaprobaciÃ³n; cobro, importe y deuda por abandono.

**PrecisiÃ³n requerida:** â€œbloques horarios por lÃ­mites del relojâ€ y â€œumbral de 180 minutos de tiempo facturableâ€ tienen bases de conteo diferentes. Los ejemplos confirman algunos casos, pero no resuelven estancias que cruzan las 08:00 ni la elecciÃ³n entre bloques civiles y duraciÃ³n acumulada. La fÃ³rmula debe describirse con ejemplos lÃ­mite aprobados antes de codificar.

## Correcciones recomendadas antes de migraciones

1. Resolver las preguntas comerciales de abandono y recargo/gracia/horarios que cambian importes o estados.
2. Completar entidades/estados de cargo excepcional en Billing y la autorizaciÃ³n correspondiente, separando saldo adeudado de pago efectivamente recibido.
3. Alinear en `01â€“06` el catÃ¡logo de `tipo_cierre`, ruta de estados y requisitos por cada forma de cierre.
4. Revisar cada regla de integridad y etiquetarla como CHECK, FK/UNIQUE, trigger local, transacciÃ³n/bloqueo de servicio o validaciÃ³n entre servicios.
5. Definir versiÃ³n mÃ­nima/objetivo de PostgreSQL y probar con ella las construcciones elegidas, incluidas extensiones si se proponen.
6. Especificar versionado inicial de migraciones por cada base, instalaciÃ³n en bases vacÃ­as, privilegios y datos iniciales idempotentes. El orden Identity/Parking/Billing no crea una FK global; los servicios deben tolerar disponibilidad parcial.
7. Ejecutar despuÃ©s una revisiÃ³n de consistencia cruzada y una prueba de integraciÃ³n con bases PostgreSQL descartables; esto serÃ¡ una etapa futura autorizada, no parte de esta auditorÃ­a.

## Lista de comprobaciÃ³n para autorizar migraciones

- [ ] Todas las decisiones de negocio que alteran tablas, estados o cobros estÃ¡n contestadas y copiadas a `06`.
- [ ] `07` solo conserva preguntas realmente abiertas, sin contradicciones ni numeraciÃ³n rota.
- [ ] `01â€“06` usan los mismos nombres, estados, relaciones y dueÃ±os de datos.
- [ ] Abandono tiene ciclo operativo, autorizaciÃ³n, cargo y deuda definidos; retiro fÃ­sico y cierre son recuperables e idempotentes.
- [ ] No existen FK entre `db_parking`, `db_identity` y `db_billing`.
- [ ] Cada integridad tiene mecanismo PostgreSQL realizable y versiÃ³n objetivo fijada.
- [ ] AprobaciÃ³n de pago, liquidaciÃ³n pagada, caja, movimiento y comprobante son atÃ³micos dentro de Billing.
- [ ] Se definieron privilegios separados de aplicaciÃ³n y migraciÃ³n y polÃ­ticas de datos histÃ³ricos.
- [ ] Los scripts se revisaron estÃ¡ticamente y se probaron en PostgreSQL descartable de la versiÃ³n objetivo.
- [ ] El propietario del proyecto aprueba el diseÃ±o final antes de crear/ejecutar migraciones.

## Resultado

El diseÃ±o tiene una separaciÃ³n de servicios razonable y varias decisiones importantes ya documentadas, pero todavÃ­a no estÃ¡ preparado para migraciones definitivas. Los hallazgos C-1 a C-4 y los pendientes comerciales indicados deben resolverse primero. Esta auditorÃ­a no autoriza ejecuciÃ³n SQL ni cambios de backend.
