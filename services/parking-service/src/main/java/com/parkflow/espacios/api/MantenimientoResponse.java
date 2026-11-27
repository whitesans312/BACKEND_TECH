package com.parkflow.espacios.api;

import java.time.Instant;
import java.util.UUID;

public record MantenimientoResponse(
        UUID id,
        UUID espacioId,
        String codigoEspacio,
        String motivo,
        String descripcion,
        String estado,
        UUID responsableId,
        Instant iniciadoEn,
        Instant finalizadoEn) {
}