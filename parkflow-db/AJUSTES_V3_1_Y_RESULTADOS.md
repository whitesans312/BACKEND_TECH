# ParkFlow · Ajustes v3.1 y resultados de pruebas en PostgreSQL 16

Cambios de arquitectura respecto a la v3: **ninguno** (10 tablas, 3 servicios, 3 bases).

## 1. Precisión 1 · Liquidaciones simultáneas con claves distintas (Billing)
Dos solicitudes con `idempotency_key` diferente para la misma `sesion_id`:
1. A inserta el pago PENDIENTE primero y confirma.
2. B choca con el índice único parcial `uq_pago_sesion_vigente` (error 23505).
3. PostgreSQL deja abortada la transacción de B: **Billing revierte toda la transacción, abre una nueva**, consulta el pago PENDIENTE/APROBADO de esa sesión y lo devuelve. Nunca sigue en la transacción abortada.
4. Si la restricción violada es `uq_pagos_idempotency` (misma clave), se aplica RN-B6: mismo `request_hash` → devolver el original; hash distinto → 409.
5. Se distingue una de otra por el nombre de la restricción. Un pago RECHAZADO no bloquea un nuevo intento.
Probado: `tests/billing_test.sql` (ambos errores) y comportamiento de espera/pérdida en `tests/concurrencia.sh`.

## 2. Precisión 2 · Comprobación diferida de comprobantes (R2)
Implementada como **constraint trigger** `ctrg_pago_aprobado_con_comprobante` (`AFTER INSERT OR UPDATE ON pagos`, `DEFERRABLE INITIALLY DEFERRED`). Al confirmar, todo pago APROBADO debe tener comprobante. La transacción única de Java sigue siendo la garantía principal.
Probado: aprobar sin comprobante falla al confirmar; aprobar + comprobante en la misma transacción pasa.

## 3. Hallazgos de la prueba (requieren tu decisión o revisión)
1. **Error en el ejemplo de la revisión de ChatGPT:** la tabla decía que 130 min cuestan $15.000. Con la fórmula aprobada, 130 − 10 de gracia = 120 min = exactamente 2 horas → **$10.000**. Pasa a $15.000 desde 131 min. El diseño y los scripts son correctos; el ejemplo estaba mal.
2. **`usuario_roles` necesita DELETE.** La regla "ningún usuario de aplicación tiene DELETE" impide quitarle un rol a un usuario. Dejé `GRANT DELETE ON usuario_roles` solo para `identity_app` (comentado en `V2__grants.sql`). Alternativa: no permitir quitar roles en v1. **Decisión pendiente.**
3. **Endurecimiento no previsto en el diseño:** el trigger de inserción de sesiones toma `FOR SHARE` sobre el espacio y su piso. Sin eso, deshabilitar un espacio mientras otra transacción abría un ticket en él era posible si la aplicación no bloqueaba la fila. Con el bloqueo, PostgreSQL serializa ambas operaciones (prueba D). La asignación con `FOR UPDATE SKIP LOCKED` sigue igual.
4. El diseño no define cómo se llena `updated_at`: lo actualiza Java (sin trigger), como se acordó. Las pruebas no lo verifican.
5. `fecha_salida` y `fecha_corte` no tienen relación forzada entre bases (no puede haberla); la comparación la hace Parking en Java (RN-P8).

## 4. Qué se probó (todas pasan)
| Archivo | Cubre |
|---|---|
| `tests/parking_test.sql` | contadores por piso, RN-P1/P2/P4, tipo vehículo-espacio, ciclo de vida de la sesión, formatos, FK, sin DELETE |
| `tests/billing_test.sql` | fórmula de tarifa, idempotencia, CHECKs de importe y estado, inmutabilidad de pagos/tarifas/comprobantes, R2, sin DELETE |
| `tests/identity_test.sql` | roles de referencia, correo único sin distinguir mayúsculas, hash no vacío, roles repetidos, permisos |
| `tests/concurrencia.sh` | dos sesiones reales: SKIP LOCKED, carrera por el mismo espacio, cierres simultáneos, deshabilitar vs. asignar |

Limitaciones: se probó en PostgreSQL 16 local, no en Azure; Flyway no se ejecutó (los archivos `V*.sql` se aplicaron en orden con `psql`); la lógica Java (reintentos, hash, JWT, clave de servicio) no está probada porque no existe todavía.

## 5. Cómo ejecutarlo
`./run_local.sh` recrea las tres bases (esquema + datos de prueba) y `./run_tests.sh` ejecuta todas las pruebas. Usuarios de prueba: `admin@parkflow.test` / `Admin123!` y `operador@parkflow.test` / `Operador123!` (solo local). Las tarifas de prueba son ficticias.
