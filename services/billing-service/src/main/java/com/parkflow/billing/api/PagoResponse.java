package com.parkflow.billing.api;

import com.parkflow.billing.domain.EstadoPago;
import com.parkflow.billing.domain.MetodoPago;
import com.parkflow.billing.domain.Pago;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PagoResponse(UUID id, UUID liquidacionId, UUID sesionParqueoId, UUID sesionCajaId,
                           String numeroTiquete, EstadoPago estado, MetodoPago metodo, BigDecimal monto,
                           String moneda, UUID operadorId, Instant aprobadoEn, Instant creadoEn,
                           ComprobanteResponse comprobante) {
    public static PagoResponse from(Pago p, ComprobanteResponse receipt) {
        return new PagoResponse(p.getId(), p.getLiquidacion().getId(), p.getLiquidacion().getSesionParqueoId(),
                p.getSesionCajaId(), p.getLiquidacion().getNumeroTiquete(), p.getEstado(), p.getMetodo(),
                p.getMonto(), "COP", p.getOperadorId(), p.getAprobadoEn(), p.getCreadoEn(), receipt);
    }
}
