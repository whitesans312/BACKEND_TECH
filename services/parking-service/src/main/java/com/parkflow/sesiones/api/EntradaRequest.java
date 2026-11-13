package com.parkflow.sesiones.api;
import com.parkflow.espacios.domain.TipoEspacio;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EntradaRequest(@NotBlank @Size(max=10) String placa, @NotNull TipoEspacio tipoVehiculo,
                             @NotNull java.util.UUID espacioId) {}
