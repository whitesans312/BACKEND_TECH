package com.parkflow.pisos.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PisoRequest(
        @NotBlank
        @Size(max = 10)
        @Pattern(regexp = "^(?!\\s)(?!.*\\s$).+$", message = "no debe tener espacios al inicio o al final")
        String codigo,

        @NotBlank
        @Size(max = 60)
        String nombre,

        @NotNull
        @Min(-5)
        @Max(32767)
        Integer orden) {
}
