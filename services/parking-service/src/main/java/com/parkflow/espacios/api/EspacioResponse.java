package com.parkflow.espacios.api;

import java.time.Instant;
import java.util.UUID;

import com.parkflow.espacios.domain.TipoEspacio;
import com.parkflow.espacios.infrastructure.EspacioAvailabilityView;

public record EspacioResponse(
        UUID id,
        UUID pisoId,
        String pisoCodigo,
        String codigo,
        TipoEspacio tipo,
        boolean habilitado,
        boolean ocupado,
        boolean disponible,
        Instant createdAt,
        Instant updatedAt) {

    public static EspacioResponse from(EspacioAvailabilityView view) {
        TipoEspacio type = TipoEspacio.valueOf(view.getTipo());
        boolean available = view.getHabilitado() && view.getPisoHabilitado() && !view.getOcupado();
        return new EspacioResponse(view.getId(), view.getPisoId(), view.getPisoCodigo(), view.getCodigo(), type,
                view.getHabilitado(), view.getOcupado(), available, view.getCreatedAt(), view.getUpdatedAt());
    }
}
