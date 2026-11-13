package com.parkflow.configuracion.api;

import com.parkflow.espacios.domain.TipoEspacio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ParqueaderoSetupRequest(
        @NotBlank @Size(max = 10) String codigoPiso,
        @NotBlank @Size(max = 60) String nombrePiso,
        @NotNull @Min(-5) @Max(32767) Integer nivel,
        @NotEmpty @Valid List<ZonaSetup> zonas) {

    public record ZonaSetup(
            @NotBlank @Size(max = 15) String codigo,
            @NotBlank @Size(max = 80) String nombre,
            @NotNull @Min(0) @Max(2000) Integer espaciosCarro,
            @NotNull @Min(0) @Max(2000) Integer espaciosMoto) {}
}
