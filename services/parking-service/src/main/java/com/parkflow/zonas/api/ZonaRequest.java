package com.parkflow.zonas.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ZonaRequest(@NotNull UUID pisoId, @NotBlank @Size(max=15) String codigo,
                          @NotBlank @Size(max=80) String nombre, String descripcion) {}
