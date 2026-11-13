# language: es
@HU-03 @E2
Característica: Configuración física del parqueadero
  Como administrador, quiero configurar pisos, zonas y espacios.

  Escenario: Crear configuración compuesta
    Dado que el administrador está autenticado
    Cuando envía un piso con sus zonas y la cantidad de espacios por tipo (CARRO o MOTO)
    Entonces se crean el piso, las zonas y los espacios asociados

  Escenario: Deshabilitar un espacio
    Dado que existe un espacio disponible
    Cuando el administrador lo deshabilita
    Entonces el espacio deja de ofrecerse para nuevas entradas
