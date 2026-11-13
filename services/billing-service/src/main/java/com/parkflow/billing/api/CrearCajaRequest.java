package com.parkflow.billing.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearCajaRequest(@NotBlank @Size(max=20) String codigo,@NotBlank @Size(max=80) String nombre,
                               @Size(max=2000) String descripcion) { }
