package com.parkflow.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.parkflow.billing.api.RechazarPagoRequest;
import com.parkflow.billing.api.ResolverPagoRequest;
import com.parkflow.billing.domain.*;
import com.parkflow.billing.infrastructure.ComprobanteRepository;
import com.parkflow.billing.infrastructure.PagoRepository;
import com.parkflow.billing.infrastructure.SesionCajaRepository;
import com.parkflow.billing.shared.ConflictException;
import com.parkflow.billing.shared.UnprocessableOperationException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class ResolucionPagoServiceTest {
    private PagoRepository pagos; private ComprobanteRepository receipts; private SesionCajaRepository sessions;
    private JdbcTemplate jdbc; private ResolucionPagoService service; private UUID paymentId,operatorId,cashSessionId;
    private Pago pending; private Liquidacion liquidation; private SesionCaja openCash;
    private final AtomicReference<Comprobante> receiptHolder=new AtomicReference<>();
    private final Instant now=Instant.parse("2026-11-05T15:00:00Z");

    @BeforeEach void setUp(){
        pagos=mock(PagoRepository.class);receipts=mock(ComprobanteRepository.class);sessions=mock(SesionCajaRepository.class);jdbc=mock(JdbcTemplate.class);
        service=new ResolucionPagoService(pagos,receipts,sessions,jdbc,Clock.fixed(now,ZoneOffset.UTC));
        paymentId=UUID.randomUUID();operatorId=UUID.randomUUID();cashSessionId=UUID.randomUUID();
        Instant entry=now.minusSeconds(130*60L);
        liquidation=new Liquidacion(UUID.randomUUID(),"PF-2026-000001","ABC123",TipoVehiculo.CARRO,entry,now,130,(short)15,115,new BigDecimal("12000.00"),UUID.randomUUID(),"a".repeat(64),operatorId);
        ReflectionTestUtils.setField(liquidation,"id",UUID.randomUUID());
        pending=new Pago(liquidation,UUID.randomUUID(),operatorId);ReflectionTestUtils.setField(pending,"id",paymentId);
        openCash=new SesionCaja(UUID.randomUUID(),operatorId,BigDecimal.ZERO,"inicio",UUID.randomUUID(),now);
        ReflectionTestUtils.setField(openCash,"id",cashSessionId);
        when(pagos.lockById(paymentId)).thenReturn(Optional.of(pending));
        when(pagos.saveAndFlush(any(Pago.class))).thenAnswer(inv->inv.getArgument(0));
        when(sessions.lockById(cashSessionId)).thenReturn(Optional.of(openCash));
        when(jdbc.queryForObject("select nextval('seq_comprobante')",Long.class)).thenReturn(1L);
        when(receipts.saveAndFlush(any(Comprobante.class))).thenAnswer(inv->{Comprobante c=inv.getArgument(0);receiptHolder.set(c);return c;});
        when(receipts.findByPago_Id(paymentId)).thenAnswer(inv->Optional.ofNullable(receiptHolder.get()));
    }

    @Test void approvesCashAndRecordsMovementAndReceiptInSameFlow(){
        var result=service.aprobar(paymentId,new ResolverPagoRequest(MetodoPago.EFECTIVO,cashSessionId),operatorId);
        assertThat(result.estado()).isEqualTo(EstadoPago.APROBADO);assertThat(result.comprobante()).isNotNull();
        assertThat(result.comprobante().numero()).isEqualTo("PF-CMP-2026-000001");
        InOrder order=inOrder(pagos,jdbc,receipts);
        order.verify(pagos).saveAndFlush(pending);
        order.verify(jdbc).update(org.mockito.ArgumentMatchers.contains("update liquidaciones set estado='PAGADA'"),any(),any(),any());
        order.verify(jdbc).queryForObject("select nextval('seq_comprobante')",Long.class);
        order.verify(jdbc).update(org.mockito.ArgumentMatchers.contains("insert into movimientos_caja"),any(Object[].class));
        order.verify(receipts).saveAndFlush(any(Comprobante.class));
    }

    @Test void rejectsCardPaymentAndLeavesItPending(){
        assertThatThrownBy(()->service.aprobar(paymentId,new ResolverPagoRequest(MetodoPago.TARJETA,cashSessionId),operatorId))
                .isInstanceOf(UnprocessableOperationException.class);
        assertThat(pending.getEstado()).isEqualTo(EstadoPago.PENDIENTE);verify(sessions,never()).lockById(any());
    }
    @Test void rejectsFreeMethodForAChargeablePayment(){
        assertThatThrownBy(()->service.aprobar(paymentId,new ResolverPagoRequest(MetodoPago.SIN_COSTO,cashSessionId),operatorId))
                .isInstanceOf(UnprocessableOperationException.class);
    }
    @Test void rejectsAClosedCashSession(){
        ReflectionTestUtils.setField(openCash,"estado","CERRADA");
        assertThatThrownBy(()->service.aprobar(paymentId,new ResolverPagoRequest(MetodoPago.EFECTIVO,cashSessionId),operatorId))
                .isInstanceOf(ConflictException.class);
    }
    @Test void approvedPaymentCannotBeApprovedThroughAnotherCashSession(){
        pending.approve(cashSessionId,operatorId,now);
        UUID another=UUID.randomUUID();
        assertThatThrownBy(()->service.aprobar(paymentId,new ResolverPagoRequest(MetodoPago.EFECTIVO,another),operatorId))
                .isInstanceOf(ConflictException.class);
    }
    @Test void rejectedPaymentIsTerminal(){
        pending.reject(operatorId,"No se recibió efectivo",now);
        assertThatThrownBy(()->service.aprobar(paymentId,new ResolverPagoRequest(MetodoPago.EFECTIVO,cashSessionId),operatorId))
                .isInstanceOf(ConflictException.class);
    }
    @Test void storesReasonWhenRejectingPendingPayment(){
        var response=service.rechazar(paymentId,new RechazarPagoRequest(" No se recibió efectivo "),operatorId);
        assertThat(response.estado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(pending.getMotivoRechazo()).isEqualTo("No se recibió efectivo");
    }
}
