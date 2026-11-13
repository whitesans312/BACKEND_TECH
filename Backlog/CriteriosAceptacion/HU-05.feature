# language: es
@HU-05 @E3
Característica: Registro de entrada
  Como operador, quiero registrar un vehículo y asignarle un espacio compatible.

  Escenario: Entrada exitosa
    Dado un espacio disponible, habilitado y compatible con el tipo de vehículo
    Cuando el operador registra placa, tipo y espacio con una Idempotency-Key
    Entonces se crea la sesión ACTIVA y se ocupa el espacio
    Y se devuelve el número de tiquete

  Escenario: Espacio ocupado o incompatible
    Dado un espacio ocupado, deshabilitado o de otro tipo
    Cuando el operador intenta asignarlo
    Entonces la entrada es rechazada y no se crea la sesión

  Escenario: Reintento con la misma clave
    Dado que una entrada ya fue registrada con una Idempotency-Key
    Cuando se repite la solicitud con la misma clave
    Entonces se devuelve el resultado original sin duplicar la sesión
