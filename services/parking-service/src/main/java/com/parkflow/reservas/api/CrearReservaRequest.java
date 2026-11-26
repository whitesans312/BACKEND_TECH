package com.parkflow.reservas.api;

import com.parkflow.espacios.domain.TipoEspacio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CrearReservaRequest(
        @NotBlank @Size(max = 10) String placa,
        @NotNull TipoEspacio tipoVehiculo,
        @NotNull UUID espacioId,
        @NotNull Instant fechaInicio,
        @NotNull Instant fechaFin,
        @Size(max = 255) String observacion) {
}
