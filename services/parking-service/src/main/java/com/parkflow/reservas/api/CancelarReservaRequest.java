package com.parkflow.reservas.api;

import jakarta.validation.constraints.Size;

public record CancelarReservaRequest(@Size(max = 255) String motivo) {
}
