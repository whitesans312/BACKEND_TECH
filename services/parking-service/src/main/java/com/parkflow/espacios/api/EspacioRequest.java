package com.parkflow.espacios.api;

import java.util.UUID;

import com.parkflow.espacios.domain.TipoEspacio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EspacioRequest(
        @NotNull UUID zonaId,

        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "^(?!\\s)(?!.*\\s$).+$", message = "no debe tener espacios al inicio o al final")
        String codigo,

        @NotNull TipoEspacio tipo) {
}
