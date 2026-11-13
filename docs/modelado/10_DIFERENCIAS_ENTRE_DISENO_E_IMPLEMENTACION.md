# Diferencias entre el diseño documentado y la implementación

## Criterio de comparación

Se contrastaron las migraciones actuales de `parkflow-db-definitivo`, los controladores/servicios/entidades Java, `docker-compose.parkflow.yml`, POM y la hoja de ruta. `parkflow-db-nuevo/` se conserva como referencia histórica, no como fuente estructural vigente; los servicios cargan `parkflow-db-definitivo/`.

| Área | Documentos/diseño | Implementación observada | Consecuencia documental |
|---|---|---|---|
| Tablas y entidades | El modelo relacional presenta tablas de expedientes, anulaciones y autorizaciones. | Migraciones definen 33 tablas; solo se encontraron 13 tablas mapeadas como entidades JPA o join table: Parking 4, Identity 3, Billing 7. | DDL no demuestra que un flujo o endpoint exista. Se identifica qué persiste solo en SQL. |
| Flujos excepcionales | Se describen salidas autorizadas, anulaciones y abandono completo. | Roadmap señala que los flujos no están implementados de extremo a extremo. | Se marcan como estructura de datos/propuesta parcial, no funcionalidad disponible. |
| Entrada | En documentos iniciales se menciona asignación automática. | `EntradaRequest` requiere `espacioId`; SesionService bloquea y valida el espacio seleccionado. | La asignación es manual. |
| Frontend | El avance académico inicial dice que se desarrollará después. | Existe proyecto React/TypeScript y servicio frontend en Compose. | La afirmación quedó desactualizada. |
| Versiones migratorias | README/roadmap presentan listas de migración diferentes. | Árbol actual contiene Parking V1–V5, Identity V1–V3, Billing V1–V4. Roadmap dice que la validación temporal llegó a Parking V1–V4. | Confirmar validación de V5 y actualizar referencias que aún indiquen V1–V3 o V1–V4 como actuales. |
| Piso | V1 imponía número mayor que cero. | V5 reemplaza por `numero >= -5`; permite sótanos desde -5 y nivel 0, sin límite superior. | Coincide con mínimo -5, pero no impone máximo para pisos positivos. |
| Rutas | Diseño sugiere API uniforme/versionada. | Pisos/espacios están bajo `/api/v1`, zonas `/api/zonas`, sesiones `/api/sesiones`. | Variación observada; verificar intención antes de fijar contrato público. |
| Transaction boundary | Regla arquitectónica dice no mantener transacción DB durante HTTP. | `SesionService.solicitarSalida` es `@Transactional`, bloquea sesión y luego llama Billing con RestClient. | Transacción/lock puede permanecer durante llamada remota. Diferencia confirmada en código. |
| Integración Identity | El DDL tiene autorizaciones de salida/cargo. | Controladores visibles incluyen auth, usuarios, roles y JWKS; no hay controller de autorización en el código listado. Roadmap reporta integración pendiente. | No afirmar autorización excepcional operativa por la existencia de la tabla. |
| Billing abandono | DDL posee cargo y eventos de estado. | No se encontraron entidades JPA ni controlador de cargo en listado. El roadmap indica flujo pendiente. | Cargo se documenta como modelo DDL no integrado. |
| API interna | La ruta tiene prefijo `/internal/v1`. | Billing Security autoriza esas rutas con roles normales ADMINISTRADOR/OPERADOR; Parking transmite Bearer JWT. | `internal` es convención de ruta, no mecanismo de autenticación de servicio distinto. |
| Manejo de fallos | Diseño describe reintento idempotente. | Parking consulta por clave; 404 crea; otros errores devuelven conflicto y solicitan reintento con misma clave. La sesión se cierra solo al obtener PAGADA/GRATUITA. | Comportamiento implementado, con dependencia temporal Parking→Billing. |
| Billing tarifa | Se carga valor de recargo y la regla comercial permanece pendiente. | Compose expone `PARKFLOW_GRACE_MINUTES`; seed parametriza recargo deshabilitado. El cálculo consultado usa tarifa horaria y diaria. | No describir recargo como cobro automático. |
| Seguridad | Documentación general puede decir que todas las rutas exigen JWT. | Health, login/register y JWKS son públicos; roles administrativos/operativos están definidos por ruta. | Enumerar excepciones reales por endpoint. |
| Prueba de DDL | El README más reciente describe validación en PostgreSQL temporal. | V5 está en el árbol local y queda fuera del rango de migraciones validado que reporta el roadmap. | Estado de validación incompleto para la última migración añadida. |

## Diferencias entre modelo SQL y JPA

### Parking

Entidades JPA: Piso, Zona, Espacio, SesionParqueo. `Piso` no mapea la columna nullable `descripcion` de `pisos`; las entidades Zona/Espacio sí incluyen descripción. No se encontraron entidades para anulaciones, solicitudes/salidas excepcionales, mantenimiento, eventos, traslados, casos/evidencias/contactos/retiros. `sesiones_parqueo` mapea sus columnas principales de cierre/idempotencia; varios actores y IDs Billing/Identity son UUID escalares externos, no asociaciones JPA.

### Identity

Entidades JPA: Usuario y Rol con relación `@ManyToMany` mediante tabla de unión `usuario_roles`. La entidad `Usuario` no mapea `intentos_fallidos`, `bloqueado_hasta` ni `ultimo_login`, columnas existentes en `usuarios`. La columna `asignado_por` de la tabla de unión no se expone como entidad auditora de asignación. No se encontraron entidades para lock de bootstrap, autorizaciones o auditoría.

### Billing

Entidades JPA: Caja, Comprobante, DetalleLiquidacion, Liquidacion, Pago, SesionCaja, Tarifa. `DetalleLiquidacion` no mapea las columnas JSONB `snapshot_tarifa` y `datos_calculo` aunque están en DDL. No se encontraron entidades para parámetros de facturación, cargo/eventos de abandono ni anulaciones de liquidación. El SQL puede operar con JDBC donde exista código, pero no se debe inferir su API completa.

## Información aún no verificada

- Contratos OpenAPI o especificación generada: no se identificó archivo OpenAPI en las rutas inspeccionadas; el inventario de endpoints se obtiene directamente de Controllers.
- Compatibilidad integral de cada anotación JPA con todos los CHECK, triggers, FK compuestas y defaults: requiere cotejo de cada atributo Java con el DDL; la lista de entidades se documenta por separado.
- Exposición real externa, certificados/TLS y reglas de firewall: Compose es local y no describe una plataforma pública.
- Ejecución efectiva de migración Parking V5: no se registra validación temporal de esa versión en roadmap.
- Cobertura de cada endpoint por tests: no se infiere únicamente de la presencia de pruebas en carpeta.

## Inventario estructural resultante

| Base | Tablas en migraciones | FK locales declaradas | Secuencias |
|---|---:|---:|---:|
| `db_parking` | 14 | 16 | 1 (`seq_ticket`) |
| `db_identity` | 7 | 8 | 0 |
| `db_billing` | 12 | 13 | 1 (`seq_comprobante`) |
| **Total** | **33** | **37** | **2** |

Conteo de FK a partir de cláusulas declaradas en DDL; restricciones compuestas cuentan como una FK. El conteo no incluye UUID externos, índices ni validaciones hechas en triggers.
