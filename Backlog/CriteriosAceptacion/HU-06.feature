# language: es
@HU-06 @E4
Característica: Liquidación de la estadía
  Como operador, quiero calcular el valor de la estadía cuando el cliente solicita salir.

  Escenario: Dentro de los 15 minutos de gracia
    Dado una sesión activa con 10 minutos de permanencia
    Cuando el operador solicita la salida
    Entonces la liquidación tiene total 0 y estado GRATUITA

  Escenario: Hasta 180 minutos facturables
    Dado un carro con 2 horas y 15 minutos de permanencia
    Cuando se solicita la liquidación
    Entonces se cobran las horas facturables a $6.000 por hora

  Escenario: Más de 180 minutos facturables
    Dado un carro con más de 180 minutos facturables
    Cuando se solicita la liquidación
    Entonces se aplica la tarifa diaria completa de $18.000 en lugar del cobro por horas
