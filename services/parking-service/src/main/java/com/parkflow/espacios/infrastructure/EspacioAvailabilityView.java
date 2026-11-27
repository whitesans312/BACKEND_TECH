package com.parkflow.espacios.infrastructure;

import java.time.Instant;
import java.util.UUID;

public interface EspacioAvailabilityView {

    UUID getId();
    UUID getPisoId();
    UUID getZonaId();
    String getPisoCodigo();
    String getCodigo();
    String getTipo();
    boolean getHabilitado();
    boolean getPisoHabilitado();
    boolean getOcupado();
    boolean getReservado();
    boolean getEnMantenimiento();
    Instant getCreatedAt();
    Instant getUpdatedAt();
}