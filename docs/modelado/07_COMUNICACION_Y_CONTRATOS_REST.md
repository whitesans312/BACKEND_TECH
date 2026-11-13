# Comunicación entre microservicios y contratos REST

## Inventario de endpoints verificados en Controllers

Los paths se transcriben de las anotaciones de los controladores; no se infiere un prefijo global adicional. Las respuestas son DTOs declarados por controlador. “Bearer JWT” es el mecanismo normal salvo health/JWKS/registro/login. Los roles provienen de las configuraciones de seguridad.

### Identity Service (8081)

| Método | Endpoint | Descripción | Autenticación / rol | Entrada principal | Respuesta |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | Registro/bootstrap de cuenta según reglas del servicio. | Público en SecurityConfiguration; el servicio aplica sus controles de registro. | `RegistroRequest`: nombre, email, password. | 201 `UsuarioResponse`. |
| POST | `/api/auth/login` | Autenticar usuario y emitir token. | Público. | `LoginRequest`: email, password. | `TokenResponse`. |
| GET | `/api/auth/me` | Cuenta del sujeto del token. | JWT válido. | Claims JWT. | `UsuarioResponse`. |
| GET | `/.well-known/jwks.json` | Publicar clave pública para validadores. | Público. | Ninguna. | JSON Web Key Set. |
| GET | `/api/roles` | Listar roles. | ADMINISTRADOR. | Ninguna. | Lista `RolResponse`. |
| POST | `/api/users` | Crear usuario y asignar roles. | ADMINISTRADOR. | `CrearUsuarioRequest`: usuario + `rolIds`. | 201 `UsuarioResponse`. |
| GET | `/api/users` | Listar usuarios. | ADMINISTRADOR. | Ninguna. | Lista `UsuarioResponse`. |
| GET | `/api/users/{id}` | Consultar usuario. | ADMINISTRADOR. | UUID de usuario. | `UsuarioResponse`. |
| PUT | `/api/users/{id}` | Actualizar usuario. | ADMINISTRADOR. | `ActualizarUsuarioRequest`: nombre y email. | `UsuarioResponse`. |
| PATCH | `/api/users/{id}/status` | Cambiar estado. | ADMINISTRADOR. | `EstadoUsuarioRequest`: estado. | `UsuarioResponse`. |
| PUT | `/api/users/{id}/roles` | Reemplazar roles. | ADMINISTRADOR. | `RolesUsuarioRequest`: conjunto `rolIds`. | `UsuarioResponse`. |

### Parking Service (8080)

| Método | Endpoint | Descripción | Autenticación / rol | Entrada principal | Respuesta |
|---|---|---|---|---|---|
| GET | `/api/v1/health` | Healthcheck de aplicación. | Público. | Ninguna. | Respuesta de salud. |
| POST | `/api/v1/parqueadero/configuracion` | Crear configuración compuesta de parqueadero. | ADMINISTRADOR. | `ParqueaderoSetupRequest`: código/nombre/nivel de piso y lista de zonas con cantidades de espacios por tipo. | 201 `ParqueaderoSetupResponse`. |
| POST | `/api/v1/pisos` | Crear piso. | ADMINISTRADOR. | `PisoRequest`: código, nombre, orden (validado entre -5 y 32767). | 201 `PisoResponse`. |
| GET | `/api/v1/pisos` | Listar pisos. | ADMINISTRADOR u OPERADOR. | Ninguna. | Lista `PisoResponse`. |
| GET | `/api/v1/pisos/{id}` | Obtener piso. | ADMINISTRADOR u OPERADOR. | UUID. | `PisoResponse`. |
| PUT | `/api/v1/pisos/{id}` | Actualizar piso. | ADMINISTRADOR. | `PisoRequest`: código, nombre, orden. | `PisoResponse`. |
| PATCH | `/api/v1/pisos/{id}/habilitado` | Habilitar/deshabilitar. | ADMINISTRADOR. | `PisoStatusRequest`. | `PisoResponse`. |
| POST | `/api/zonas` | Crear zona. | ADMINISTRADOR. | `ZonaRequest`: `pisoId`, código, nombre y campos opcionales del DTO. | 201 `ZonaResponse`. |
| GET | `/api/zonas?pisoId={uuid}` | Listar zonas, filtrando opcionalmente por piso. | ADMINISTRADOR u OPERADOR. | `pisoId` opcional. | Lista `ZonaResponse`. |
| PUT | `/api/zonas/{id}` | Actualizar zona. | ADMINISTRADOR. | `ZonaRequest`: `pisoId`, código, nombre y campos opcionales del DTO. | `ZonaResponse`. |
| PATCH | `/api/zonas/{id}/habilitada` | Habilitar/deshabilitar zona. | ADMINISTRADOR. | `ZonaStatusRequest`. | `ZonaResponse`. |
| POST | `/api/v1/espacios` | Crear espacio. | ADMINISTRADOR. | `EspacioRequest`: `zonaId`, código, tipo (`CARRO`/`MOTO`). | 201 `EspacioResponse`. |
| GET | `/api/v1/espacios?pisoId={uuid}` | Listar espacios y filtrar opcionalmente por piso. | ADMINISTRADOR u OPERADOR. | `pisoId` opcional. | Lista `EspacioResponse`. |
| GET | `/api/v1/espacios/{id}` | Obtener espacio. | ADMINISTRADOR u OPERADOR. | UUID. | `EspacioResponse`. |
| PUT | `/api/v1/espacios/{id}` | Actualizar espacio. | ADMINISTRADOR. | `EspacioRequest`: `zonaId`, código, tipo. | `EspacioResponse`. |
| PATCH | `/api/v1/espacios/{id}/habilitado` | Habilitar/deshabilitar espacio. | ADMINISTRADOR. | `EspacioStatusRequest`. | `EspacioResponse`. |
| POST | `/api/sesiones/entradas` | Registrar entrada. | JWT; sujeto se usa como operador. Requiere `Idempotency-Key`. | `EntradaRequest`: placa, tipo vehículo, espacio UUID elegido manualmente. | 201 `SesionResponse`. |
| GET | `/api/sesiones?activas={bool}` | Listar sesiones; `activas` default false. | ADMINISTRADOR u OPERADOR. | Query opcional. | Lista `SesionResponse`. |
| GET | `/api/sesiones/{ticket}` | Consultar por tiquete. | ADMINISTRADOR u OPERADOR. | Tiquete. | `SesionResponse`. |
| POST | `/api/sesiones/{ticket}/salida` | Solicitar salida/liquidación; usa clave para correlación e idempotencia. | JWT; requiere `Idempotency-Key`. | Ticket y JWT. | `SalidaResponse`, 200 si salida confirmada, 202 si pendiente. |
| GET | `/api/sesiones/buscar-placa?placa={texto}` | Buscar sesiones por placa. | ADMINISTRADOR u OPERADOR. | Placa. | Lista `SesionResponse`. |
| PUT | `/api/sesiones/{ticket}/placa` | Corregir placa y auditar evento. | JWT; requiere `Idempotency-Key`. | `CorreccionPlacaRequest`: placa nueva y motivo. | `SesionResponse`. |

**Variación de ruta observada:** pisos/espacios/health/configuración usan `/api/v1`, zonas y sesiones no. La ruta de configuración coincide con el matcher de seguridad `/api/v1/parqueadero/configuracion/**`. Confirmar si la falta de prefijo común es intencional antes de versionar API.

### Billing Service (8083)

| Método | Endpoint | Descripción | Autenticación / rol | Entrada principal | Respuesta |
|---|---|---|---|---|---|
| GET | `/api/billing/health` | Healthcheck de aplicación. | Público. | Ninguna. | Estado de salud. |
| GET | `/api/billing/tarifas?tipoVehiculo={tipo}` | Listar tarifas; filtro opcional. | ADMINISTRADOR u OPERADOR. | Enum opcional. | Lista `TarifaResponse`. |
| PUT | `/api/billing/tarifas/vigentes/{tipoVehiculo}/{concepto}` | Reemplazar tarifa vigente. | ADMINISTRADOR. | `ActualizarTarifaRequest`: `valor` y moneda opcional. | `TarifaResponse`. |
| GET | `/api/billing/cajas` | Listar cajas. | ADMINISTRADOR u OPERADOR. | Ninguna. | Lista `CajaResponse`. |
| POST | `/api/billing/cajas` | Crear caja. | ADMINISTRADOR. | `CrearCajaRequest`: código, nombre y descripción opcional. | 201 `CajaResponse`. |
| POST | `/api/billing/cajas/sesiones` | Abrir sesión de caja. | Bearer JWT; requiere `Idempotency-Key`. | `AbrirSesionCajaRequest`: caja UUID, saldo inicial y observación opcional; operador del JWT. | 201 `SesionCajaResponse`. |
| GET | `/api/billing/cajas/sesiones/{id}` | Obtener sesión de caja. | ADMINISTRADOR u OPERADOR. | UUID. | `SesionCajaResponse`. |
| POST | `/api/billing/cajas/sesiones/{id}/cerrar` | Cerrar sesión/arqueo. | Bearer JWT. | `CerrarSesionCajaRequest`: saldo contado y observación opcional; operador del JWT. | `SesionCajaResponse`. |
| GET | `/api/billing/pagos/{id}` | Consultar pago. | ADMINISTRADOR u OPERADOR. | UUID. | `PagoResponse`. |
| GET | `/api/billing/comprobantes/{id}` | Consultar comprobante. | ADMINISTRADOR u OPERADOR. | UUID. | `ComprobanteResponse`. |
| POST | `/internal/v1/liquidaciones` | Crear/recuperar liquidación. | ADMINISTRADOR u OPERADOR con JWT. | `CrearLiquidacionRequest`: `sesionId`, ticket, tipo, placa, entrada y corte + `Idempotency-Key`. | 201 nueva o 200 recuperada; `LiquidacionResponse`. |
| GET | `/internal/v1/liquidaciones/by-idempotency-key/{key}` | Recuperar liquidación por clave. | ADMINISTRADOR u OPERADOR con JWT. | UUID en ruta. | `LiquidacionResponse`. |
| GET | `/internal/v1/liquidaciones/{id}` | Obtener liquidación por ID. | ADMINISTRADOR u OPERADOR con JWT. | UUID. | `LiquidacionResponse`. |
| GET | `/internal/v1/liquidaciones?sesionId={uuid}` | Listar liquidaciones de sesión. | ADMINISTRADOR u OPERADOR con JWT. | `sesionId`. | Lista `LiquidacionResponse`. |
| POST | `/internal/v1/liquidaciones/{id}/cancelar` | Cancelar liquidación pendiente. | ADMINISTRADOR u OPERADOR con JWT. | `CancelarLiquidacionRequest`: motivo. | `LiquidacionResponse`. |
| POST | `/internal/v1/pagos/{id}/resolver` | Aprobar pago efectivo y completar operación financiera. | ADMINISTRADOR u OPERADOR con JWT. | `ResolverPagoRequest`: método y UUID de sesión de caja. | `PagoResponse`. |
| POST | `/internal/v1/pagos/{id}/rechazar` | Rechazar intento de pago pendiente. | ADMINISTRADOR u OPERADOR con JWT. | `RechazarPagoRequest`: motivo. | `PagoResponse`. |

La etiqueta `internal` forma parte del path y el servicio recibe llamadas Parking→Billing, pero la configuración observada autentica esas rutas mediante los mismos roles ADMINISTRADOR/OPERADOR; no se encontró un rol técnico dedicado.

## Comunicación Parking → Billing

1. Parking consulta `GET /internal/v1/liquidaciones/by-idempotency-key/{key}` con el bearer token recibido.
2. Si responde 404 y la sesión sigue activa, Parking envía `POST /internal/v1/liquidaciones` con `Idempotency-Key` y JSON con `sesionId`, `numeroTicket`, `tipoVehiculo`, `placa`, `fechaEntrada` y `fechaCorte`.
3. Billing retorna ID, estado y total dentro de `LiquidacionResponse`.
4. `PAGADA` o `GRATUITA` permite a Parking cerrar la sesión; estados pendientes dejan sesión activa y respuesta no confirmada.
5. Si una llamada falla con estado diferente de 404, o la creación falla, Parking responde conflicto y pide reintentar con la misma clave. La respuesta no se interpreta como pago confirmado.

Billing crea idempotentemente con `Idempotency-Key` y además compara `hashSolicitud`. Reusar una clave con contenido distinto da conflicto. Una unicidad parcial impide más de una liquidación pendiente por sesión.

## Observación de consistencia en el código

`SesionService.solicitarSalida` está anotado `@Transactional`, obtiene la sesión con bloqueo y ejecuta llamadas HTTP a Billing dentro del método. Esto mantiene la transacción/lock de Parking durante la llamada remota, contrario a la regla en documentos que indica no mantener una transacción de BD abierta durante HTTP. Se registra como diferencia a resolver, sin cambiar código en esta entrega.
