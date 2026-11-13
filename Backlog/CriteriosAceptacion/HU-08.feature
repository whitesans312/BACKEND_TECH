# language: es
@HU-08 @E3
Característica: Cierre de la salida
  Como operador, quiero cerrar la salida después de confirmarse el resultado de Billing.

  Escenario: Cierre con liquidación pagada o gratuita
    Dado una liquidación en estado PAGADA o GRATUITA
    Cuando el operador confirma el cierre
    Entonces Parking cierra la sesión y libera el espacio

  Escenario: Billing sin resultado terminal
    Dado que Billing no está disponible o la liquidación sigue pendiente
    Cuando el operador intenta cerrar la sesión
    Entonces la sesión permanece ACTIVA y el espacio ocupado

  Escenario: El cliente decide no salir
    Dado una liquidación pendiente
    Cuando el operador la cancela
    Entonces la liquidación pasa a CANCELADA y la sesión sigue ACTIVA
