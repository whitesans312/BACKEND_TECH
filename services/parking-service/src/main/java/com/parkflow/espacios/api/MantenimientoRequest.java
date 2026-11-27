package com.parkflow.espacios.api;

import com.parkflow.espacios.domain.MotivoMantenimiento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MantenimientoRequest(
        @NotNull MotivoMantenimiento motivo,
        @NotBlank @Size(max = 500) String descripcion) {
}