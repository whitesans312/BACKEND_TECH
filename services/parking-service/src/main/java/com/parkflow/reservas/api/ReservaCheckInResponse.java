package com.parkflow.reservas.api;

import com.parkflow.sesiones.api.SesionResponse;

public record ReservaCheckInResponse(ReservaResponse reserva, SesionResponse sesion) {
}
