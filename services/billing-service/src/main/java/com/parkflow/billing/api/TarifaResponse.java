package com.parkflow.billing.api;

import com.parkflow.billing.domain.ConceptoTarifa;
import com.parkflow.billing.domain.Tarifa;
import com.parkflow.billing.domain.TipoVehiculo;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TarifaResponse(UUID id, TipoVehiculo tipoVehiculo, ConceptoTarifa concepto, BigDecimal valor,
                             String moneda, Instant vigenteDesde, Instant vigenteHasta, boolean activa, Instant createdAt) {
    public static TarifaResponse from(Tarifa t) {
        return new TarifaResponse(t.getId(),t.getTipoVehiculo(),t.getConcepto(),t.getValor(),t.getMoneda(),
                t.getVigenteDesde(),t.getVigenteHasta(),t.isActiva(),t.getCreatedAt());
    }
}
