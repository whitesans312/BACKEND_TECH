# Queries y Actions

Se separan las operaciones que **cambian el estado** (Actions) de las que **solo consultan** (Queries). Las rutas son las del inventario de controladores.

## Actions

| Action | Servicio | Operación | Acceso | Efecto |
|---|---|---|---|---|
| Autenticar | Identity | `POST /api/auth/login` | Público | Valida credenciales y emite un JWT RS256 |
| CrearUsuario | Identity | `POST /api/users` | ADMINISTRADOR | Crea una cuenta y le asigna roles |
| ActualizarUsuario | Identity | `PUT /api/users/{id}` | ADMINISTRADOR | Cambia nombre y correo |
| CambiarEstadoUsuario | Identity | `PATCH /api/users/{id}/status` | ADMINISTRADOR | Activa o desactiva la cuenta |
| AsignarRoles | Identity | `PUT /api/users/{id}/roles` | ADMINISTRADOR | Reemplaza los roles del usuario |
| ConfigurarParqueadero | Parking | `POST /api/v1/parqueadero/configuracion` | ADMINISTRADOR | Crea piso, zonas y espacios por tipo |
| CrearPiso | Parking | `POST /api/v1/pisos` | ADMINISTRADOR | Crea un piso |
| CrearZona | Parking | `POST /api/zonas` | ADMINISTRADOR | Crea una zona dentro de un piso |
| CrearEspacio | Parking | `POST /api/v1/espacios` | ADMINISTRADOR | Crea un espacio CARRO o MOTO |
| HabilitarEspacio | Parking | `PATCH /api/v1/espacios/{id}/habilitado` | ADMINISTRADOR | Habilita o deshabilita un espacio |
| RegistrarEntrada | Parking | `POST /api/sesiones/entradas` | JWT + Idempotency-Key | Crea la sesión, ocupa el espacio y emite el tiquete |
| SolicitarSalida | Parking | `POST /api/sesiones/{ticket}/salida` | JWT + Idempotency-Key | Obtiene la liquidación de Billing; cierra la sesión si el resultado es terminal |
| CorregirPlaca | Parking | `PUT /api/sesiones/{ticket}/placa` | JWT + Idempotency-Key | Corrige la placa y audita el evento |
| CrearLiquidacion | Billing | `POST /internal/v1/liquidaciones` | ADMINISTRADOR u OPERADOR | Crea o recupera la liquidación de una sesión |
| CancelarLiquidacion | Billing | `POST /internal/v1/liquidaciones/{id}/cancelar` | ADMINISTRADOR u OPERADOR | Cancela una liquidación pendiente |
| ResolverPago | Billing | `POST /internal/v1/pagos/{id}/resolver` | ADMINISTRADOR u OPERADOR | Aprueba el pago en efectivo, registra el movimiento y el comprobante |
| RechazarPago | Billing | `POST /internal/v1/pagos/{id}/rechazar` | ADMINISTRADOR u OPERADOR | Rechaza un intento de pago pendiente |
| ActualizarTarifaVigente | Billing | `PUT /api/billing/tarifas/vigentes/{tipoVehiculo}/{concepto}` | ADMINISTRADOR | Reemplaza la tarifa vigente sin alterar el historial |
| CrearCaja | Billing | `POST /api/billing/cajas` | ADMINISTRADOR | Crea una caja lógica |
| AbrirSesionCaja | Billing | `POST /api/billing/cajas/sesiones` | JWT + Idempotency-Key | Abre una sesión de caja con saldo inicial |
| CerrarSesionCaja | Billing | `POST /api/billing/cajas/sesiones/{id}/cerrar` | JWT | Cierra la sesión de caja con el saldo contado |

## Queries

| Query | Servicio | Operación | Acceso | Resultado |
|---|---|---|---|---|
| ObtenerMiCuenta | Identity | `GET /api/auth/me` | JWT válido | Cuenta del sujeto del token |
| ObtenerJWKS | Identity | `GET /.well-known/jwks.json` | Público | Claves públicas para validar JWT |
| ListarUsuarios | Identity | `GET /api/users` | ADMINISTRADOR | Lista de usuarios |
| ObtenerUsuario | Identity | `GET /api/users/{id}` | ADMINISTRADOR | Un usuario |
| ListarRoles | Identity | `GET /api/roles` | ADMINISTRADOR | Catálogo de roles |
| ListarPisos | Parking | `GET /api/v1/pisos` | ADMINISTRADOR u OPERADOR | Lista de pisos |
| ListarZonas | Parking | `GET /api/zonas?pisoId={uuid}` | ADMINISTRADOR u OPERADOR | Zonas, con filtro opcional por piso |
| ListarEspacios | Parking | `GET /api/v1/espacios?pisoId={uuid}` | ADMINISTRADOR u OPERADOR | Espacios y su disponibilidad, con filtro opcional por piso |
| ListarSesiones | Parking | `GET /api/sesiones?activas={bool}` | ADMINISTRADOR u OPERADOR | Sesiones, por defecto todas |
| ConsultarSesionPorTiquete | Parking | `GET /api/sesiones/{ticket}` | ADMINISTRADOR u OPERADOR | Una sesión por su tiquete |
| BuscarSesionesPorPlaca | Parking | `GET /api/sesiones/buscar-placa?placa={texto}` | ADMINISTRADOR u OPERADOR | Sesiones que coinciden con la placa |
| ListarTarifas | Billing | `GET /api/billing/tarifas?tipoVehiculo={tipo}` | ADMINISTRADOR u OPERADOR | Tarifas, con filtro opcional |
| ObtenerLiquidacion | Billing | `GET /internal/v1/liquidaciones/{id}` | ADMINISTRADOR u OPERADOR | Una liquidación |
| RecuperarLiquidacionPorClave | Billing | `GET /internal/v1/liquidaciones/by-idempotency-key/{key}` | ADMINISTRADOR u OPERADOR | Liquidación asociada a una clave idempotente |
| ListarLiquidacionesDeSesion | Billing | `GET /internal/v1/liquidaciones?sesionId={uuid}` | ADMINISTRADOR u OPERADOR | Liquidaciones de una sesión |
| ObtenerPago | Billing | `GET /api/billing/pagos/{id}` | ADMINISTRADOR u OPERADOR | Un pago |
| ObtenerComprobante | Billing | `GET /api/billing/comprobantes/{id}` | ADMINISTRADOR u OPERADOR | Un comprobante |
| ListarCajas | Billing | `GET /api/billing/cajas` | ADMINISTRADOR u OPERADOR | Cajas |
| ObtenerSesionCaja | Billing | `GET /api/billing/cajas/sesiones/{id}` | ADMINISTRADOR u OPERADOR | Una sesión de caja |

> Nota: el flujo documentado antes usaba `POST /api/sesiones`; el inventario de controladores indica `POST /api/sesiones/entradas`.
