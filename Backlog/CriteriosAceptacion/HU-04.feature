# language: es
@HU-04 @E2
Característica: Disponibilidad por piso
  Como operador, quiero consultar la disponibilidad por piso.

  Escenario: Consultar espacios de un piso
    Dado que el operador está autenticado y existe un piso con espacios
    Cuando consulta los espacios filtrando por ese piso
    Entonces ve cada espacio con su tipo y su estado operativo
