package com.parkflow.sesiones.api;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrasladoRequest(
        @NotNull UUID espacioDestinoId,
        @NotBlank @Size(max = 500) String motivo) {
}