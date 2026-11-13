package com.parkflow.billing.application;

import com.parkflow.billing.api.ComprobanteResponse;
import com.parkflow.billing.api.PagoResponse;
import com.parkflow.billing.domain.EstadoPago;
import com.parkflow.billing.infrastructure.ComprobanteRepository;
import com.parkflow.billing.infrastructure.PagoRepository;
import com.parkflow.billing.shared.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaPagoService {
    private final PagoRepository pagos;
    private final ComprobanteRepository comprobantes;

    public ConsultaPagoService(PagoRepository pagos, ComprobanteRepository comprobantes) {
        this.pagos = pagos;
        this.comprobantes = comprobantes;
    }

    @Transactional(readOnly = true)
    public PagoResponse porId(UUID id) {
        var payment = pagos.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el pago solicitado."));
        return response(payment);
    }

    @Transactional(readOnly = true)
    public PagoResponse porClave(UUID key) {
        var payment = pagos.findDetailedByIdempotencyKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una liquidación para esa clave."));
        return response(payment);
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> porSesion(UUID sessionId) {
        return pagos.findDetailedBySession(sessionId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public ComprobanteResponse comprobante(UUID id) {
        return comprobantes.findById(id).map(ComprobanteResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el comprobante solicitado."));
    }

    private PagoResponse response(com.parkflow.billing.domain.Pago payment) {
        var receipt = payment.getEstado() == EstadoPago.APROBADO
                ? comprobantes.findByPago_Id(payment.getId()).map(ComprobanteResponse::from).orElse(null)
                : null;
        return PagoResponse.from(payment, receipt);
    }
}
