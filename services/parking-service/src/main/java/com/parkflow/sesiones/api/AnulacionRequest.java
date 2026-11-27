package com.parkflow.sesiones.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnulacionRequest(@NotBlank @Size(max = 500) String motivo) {
}