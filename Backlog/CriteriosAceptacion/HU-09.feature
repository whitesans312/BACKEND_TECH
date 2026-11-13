# language: es
@HU-09 @E4
Característica: Actualización de tarifas
  Como administrador, quiero actualizar tarifas sin alterar liquidaciones históricas.

  Escenario: Nueva tarifa vigente
    Dado que existen liquidaciones calculadas con la tarifa anterior
    Cuando el administrador actualiza la tarifa vigente
    Entonces las liquidaciones nuevas usan el valor actualizado
    Y las liquidaciones anteriores conservan su importe original
