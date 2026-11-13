package com.parkflow.sesiones.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CorreccionPlacaRequest(@NotBlank @Size(max=10) String placa,
                                    @NotBlank @Size(max=500) String motivo) { }
