package com.parkflow.billing.api;

import com.parkflow.billing.domain.EstadoLiquidacion;
import com.parkflow.billing.domain.Liquidacion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LiquidacionResponse(UUID id, UUID sesionParqueoId, UUID cargoAbandonoId, String numeroTiquete,
                                  String placa, String tipoVehiculo, Instant horaEntrada, Instant horaCorte,
                                  Integer minutosTotales, Short minutosGracia, Integer minutosFacturables,
                                  BigDecimal subtotal, BigDecimal recargos, BigDecimal descuentos, BigDecimal total,
                                  EstadoLiquidacion estado, PagoResponse pago, ComprobanteResponse comprobante) {
    public static LiquidacionResponse from(Liquidacion l, PagoResponse pago, ComprobanteResponse comprobante) {
        return new LiquidacionResponse(l.getId(), l.getSesionParqueoId(), l.getCargoAbandonoId(), l.getNumeroTiquete(),
                l.getPlaca(), l.getTipoVehiculo() == null ? null : l.getTipoVehiculo().name(), l.getHoraEntrada(),
                l.getHoraCorte(), l.getMinutosTotales(), l.getMinutosGracia(), l.getMinutosFacturables(),
                l.getSubtotal(), l.getRecargos(), l.getDescuentos(), l.getTotal(), l.getEstado(), pago, comprobante);
    }
}
