# Paso 3 — Comandos, actores y agregados

| Comando | Actor | Agregado | Evento(s) |
|---|---|---|---|
| Autenticar | Operador / Administrador | Usuario | SesionIniciada / InicioSesionRechazado |
| CrearUsuario, AsignarRoles | Administrador | Usuario | UsuarioCreado, RolesAsignados |
| ConfigurarParqueadero | Administrador | Piso, Zona, Espacio | PisoCreado, ZonaCreada, EspacioCreado |
| RegistrarEntrada | Operador | SesionParqueo | EntradaRegistrada, TiqueteEmitido, EspacioOcupado |
| CorregirPlaca | Operador | SesionParqueo | PlacaCorregida |
| SolicitarSalida | Operador | SesionParqueo | SalidaSolicitada |
| CrearLiquidacion | Parking (sistema) | Liquidacion | LiquidacionCreada |
| CancelarLiquidacion | Operador | Liquidacion | LiquidacionCancelada |
| ResolverPago | Operador | Pago | PagoAprobado, MovimientoCajaRegistrado, ComprobanteEmitido |
| ConfirmarCierre | Operador | SesionParqueo | SesionCerrada, EspacioLiberado |
| ActualizarTarifaVigente | Administrador | Tarifa | TarifaActualizada |
| AbrirSesionCaja, CerrarSesionCaja | Operador | SesionCaja | SesionCajaAbierta, SesionCajaCerrada |

## Políticas

| Cuando | Entonces |
|---|---|
| Cuando la liquidación queda PAGADA o GRATUITA | Parking puede cerrar la sesión y liberar el espacio |
| Cuando se aprueba un pago | Billing registra el movimiento de caja y el comprobante en la misma transacción local |
| Cuando se repite una solicitud con la misma clave idempotente | Se devuelve el resultado original sin duplicar |
| Cuando se cancela una liquidación pendiente | La sesión sigue ACTIVA y el espacio ocupado |
