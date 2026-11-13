package com.parkflow.billing.application;

import com.parkflow.billing.api.ComprobanteResponse;
import com.parkflow.billing.api.PagoResponse;
import com.parkflow.billing.api.RechazarPagoRequest;
import com.parkflow.billing.api.ResolverPagoRequest;
import com.parkflow.billing.domain.Comprobante;
import com.parkflow.billing.domain.EstadoPago;
import com.parkflow.billing.domain.MetodoPago;
import com.parkflow.billing.domain.Pago;
import com.parkflow.billing.domain.TipoComprobante;
import com.parkflow.billing.infrastructure.ComprobanteRepository;
import com.parkflow.billing.infrastructure.PagoRepository;
import com.parkflow.billing.infrastructure.SesionCajaRepository;
import com.parkflow.billing.shared.ConflictException;
import com.parkflow.billing.shared.ResourceNotFoundException;
import com.parkflow.billing.shared.UnprocessableOperationException;
import java.time.Clock;
import java.time.Year;
import java.time.ZoneId;
import java.util.UUID;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResolucionPagoService {
    private static final ZoneId BOGOTA=ZoneId.of("America/Bogota");
    private final PagoRepository pagos;
    private final ComprobanteRepository comprobantes;
    private final SesionCajaRepository sesionesCaja;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Autowired public ResolucionPagoService(PagoRepository pagos, ComprobanteRepository comprobantes,
                                 SesionCajaRepository sesionesCaja, JdbcTemplate jdbc) {
        this(pagos, comprobantes, sesionesCaja, jdbc, Clock.systemUTC());
    }
    ResolucionPagoService(PagoRepository pagos, ComprobanteRepository comprobantes,
                          SesionCajaRepository sesionesCaja, JdbcTemplate jdbc, Clock clock) {
        this.pagos=pagos; this.comprobantes=comprobantes; this.sesionesCaja=sesionesCaja; this.jdbc=jdbc; this.clock=clock;
    }

    @Transactional
    public PagoResponse aprobar(UUID id, ResolverPagoRequest request, UUID operatorId) {
        Pago payment=pagos.lockById(id).orElseThrow(() -> new ResourceNotFoundException("No existe el pago solicitado."));
        if (payment.getEstado()==EstadoPago.APROBADO) {
        if (request.sesionCajaId().equals(payment.getSesionCajaId())) return response(payment);
            throw new ConflictException("El pago ya fue aprobado en otra sesión de caja.");
        }
        if (payment.getEstado()==EstadoPago.RECHAZADO) throw new ConflictException("El pago fue rechazado y no se puede aprobar.");
        if (request.metodo()!=MetodoPago.EFECTIVO) throw new UnprocessableOperationException("ParkFlow únicamente acepta efectivo.");
        var cashSession=sesionesCaja.lockById(request.sesionCajaId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe la sesión de caja indicada."));
        if (!cashSession.isAbierta()) throw new ConflictException("La sesión de caja está cerrada.");
        if (!cashSession.getOperadorId().equals(operatorId)) throw new ConflictException("La caja está abierta por otro operador.");

        var now=clock.instant();
        payment.approve(cashSession.getId(), operatorId, now);
        pagos.saveAndFlush(payment);
        var liquidation=payment.getLiquidacion();
        jdbc.update("update liquidaciones set estado='PAGADA', resuelta_en=?, actualizado_en=? where id=?",
                Timestamp.from(now), Timestamp.from(now), liquidation.getId());
        long sequence=jdbc.queryForObject("select nextval('seq_comprobante')",Long.class);
        String number="PF-CMP-%04d-%06d".formatted(Year.from(now.atZone(BOGOTA)).getValue(),sequence);
        jdbc.update("""
                insert into movimientos_caja(id,sesion_caja_id,pago_id,liquidacion_id,operador_id,tipo_movimiento,
                    concepto,monto,clave_idempotencia,creado_en)
                values (gen_random_uuid(),?,?,?,?, 'INGRESO','PAGO_PARQUEO',?,?,?)
                """, cashSession.getId(), payment.getId(), liquidation.getId(), operatorId, payment.getMonto(), payment.getId(), Timestamp.from(now));
        comprobantes.saveAndFlush(new Comprobante(liquidation,payment,TipoComprobante.PAGO_EFECTIVO,number,now));
        return response(payment);
    }

    @Transactional
    public PagoResponse rechazar(UUID id, RechazarPagoRequest request, UUID operatorId) {
        Pago payment=pagos.lockById(id).orElseThrow(() -> new ResourceNotFoundException("No existe el pago solicitado."));
        String reason=request.motivo().trim();
        if (payment.getEstado()==EstadoPago.RECHAZADO) {
            if (reason.equals(payment.getMotivoRechazo()) && operatorId.equals(payment.getResueltoPor())) return response(payment);
            throw new ConflictException("El pago ya fue rechazado con otra resolución.");
        }
        if (payment.getEstado()==EstadoPago.APROBADO) throw new ConflictException("Un pago aprobado es inmutable.");
        payment.reject(operatorId,reason,clock.instant());
        pagos.saveAndFlush(payment);
        return response(payment);
    }

    private PagoResponse response(Pago p) {
        ComprobanteResponse receipt=comprobantes.findByPago_Id(p.getId()).map(ComprobanteResponse::from).orElse(null);
        return PagoResponse.from(p,receipt);
    }
}
