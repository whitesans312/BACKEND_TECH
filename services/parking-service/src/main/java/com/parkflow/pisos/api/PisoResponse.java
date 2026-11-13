package com.parkflow.pisos.api;

import java.time.Instant;
import java.util.UUID;

import com.parkflow.pisos.domain.Piso;

public record PisoResponse(
        UUID id,
        String codigo,
        String nombre,
        Short orden,
        boolean habilitado,
        Instant createdAt,
        Instant updatedAt) {

    public static PisoResponse from(Piso piso) {
        return new PisoResponse(piso.getId(), piso.getCodigo(), piso.getNombre(), piso.getOrden(),
                piso.isHabilitado(), piso.getCreatedAt(), piso.getUpdatedAt());
    }
}
