package com.parkflow.zonas.api;

import com.parkflow.zonas.domain.Zona;
import java.util.UUID;

public record ZonaResponse(UUID id, UUID pisoId, String pisoCodigo, String codigo, String nombre, boolean activa) {
    public static ZonaResponse from(Zona z) {
        return new ZonaResponse(z.getId(), z.getPiso().getId(), z.getPiso().getCodigo(), z.getCodigo(), z.getNombre(), z.isActiva());
    }
}
