# language: es
@HU-07 @E5
Característica: Pago en efectivo
  Como operador, quiero confirmar el efectivo recibido y dejar constancia del cobro.

  Escenario: Pago completo en efectivo
    Dado una liquidación pendiente y una sesión de caja abierta
    Cuando el operador confirma el efectivo recibido
    Entonces se registran en una sola transacción el pago aprobado, el movimiento de caja y el comprobante

  Escenario: No se aceptan pagos parciales
    Dado una liquidación pendiente
    Cuando se intenta pagar un valor menor al total
    Entonces el pago no se aprueba
