package com.parkflow.sesiones.api;

import java.time.Instant;
import java.util.UUID;
import com.parkflow.espacios.domain.TipoEspacio;
import com.parkflow.sesiones.domain.SesionParqueo;
import com.parkflow.sesiones.domain.EstadoSesion;

public record SesionResponse(UUID id, String numeroTicket, String placa, TipoEspacio tipoVehiculo, UUID espacioId, String codigoEspacio, EstadoSesion estado, Instant fechaEntrada, Instant fechaSalida, UUID operadorEntradaId) {
    public static SesionResponse from(SesionParqueo s) { return new SesionResponse(s.getId(),s.getNumeroTicket(),s.getPlacaActual(),s.getTipoVehiculo(),s.getEspacio().getId(),s.getEspacio().getCodigo(),s.getEstado(),s.getFechaEntrada(),s.getFechaSalida(),s.getOperadorEntradaId()); }
}
