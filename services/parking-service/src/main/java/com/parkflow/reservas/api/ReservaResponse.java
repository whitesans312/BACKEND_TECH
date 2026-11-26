package com.parkflow.reservas.api;

import com.parkflow.espacios.domain.TipoEspacio;
import com.parkflow.reservas.domain.EstadoReserva;
import com.parkflow.reservas.domain.Reserva;
import java.time.Instant;
import java.util.UUID;

public record ReservaResponse(
        UUID id,
        String codigoReserva,
        UUID espacioId,
        String codigoEspacio,
        String pisoCodigo,
        String placa,
        TipoEspacio tipoVehiculo,
        Instant fechaInicio,
        Instant fechaFin,
        EstadoReserva estado,
        UUID sesionId,
        String observacion,
        String motivoCancelacion,
        Instant creadoEn) {

    public static ReservaResponse from(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getCodigoReserva(),
                reserva.getEspacio().getId(),
                reserva.getEspacio().getCodigo(),
                reserva.getEspacio().getZona().getPiso().getCodigo(),
                reserva.getPlaca(),
                reserva.getTipoVehiculo(),
                reserva.getFechaInicio(),
                reserva.getFechaFin(),
                reserva.getEstado(),
                reserva.getSesionId(),
                reserva.getObservacion(),
                reserva.getMotivoCancelacion(),
                reserva.getCreadoEn());
    }
}
