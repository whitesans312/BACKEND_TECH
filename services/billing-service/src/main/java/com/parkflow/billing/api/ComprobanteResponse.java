package com.parkflow.billing.api;

import com.parkflow.billing.domain.Comprobante;
import com.parkflow.billing.domain.TipoComprobante;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ComprobanteResponse(UUID id, UUID pagoId, UUID liquidacionId, TipoComprobante tipo,
                                  String numero, BigDecimal total, Instant emitidoEn) {
    public static ComprobanteResponse from(Comprobante c) {
        return new ComprobanteResponse(c.getId(), c.getPago() == null ? null : c.getPago().getId(),
                c.getLiquidacion().getId(), c.getTipo(), c.getNumero(), c.getLiquidacion().getTotal(), c.getEmitidoEn());
    }
}
