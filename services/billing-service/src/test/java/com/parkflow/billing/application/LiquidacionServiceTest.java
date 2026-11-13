package com.parkflow.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.parkflow.billing.api.CrearLiquidacionRequest;
import com.parkflow.billing.domain.*;
import com.parkflow.billing.infrastructure.*;
import com.parkflow.billing.shared.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

class LiquidacionServiceTest {
    private LiquidacionRepository liquidaciones;
    private DetalleLiquidacionRepository detalles;
    private PagoRepository pagos;
    private TarifaRepository tarifas;
    private ComprobanteRepository comprobantes;
    private PlatformTransactionManager manager;
    private JdbcTemplate jdbc;
    private LiquidacionService service;
    private final HashSolicitud hash=new HashSolicitud();
    private final CalculadoraLiquidacion calculator=new CalculadoraLiquidacion();
    private final UUID sessionId=UUID.randomUUID(),key=UUID.randomUUID(),operatorId=UUID.randomUUID();
    private final Instant entry=Instant.parse("2026-11-02T13:00:00Z");
    private final Tarifa hourly=new Tarifa(TipoVehiculo.CARRO,ConceptoTarifa.HORA,new BigDecimal("6000.00"),"COP",entry);
    private final Tarifa daily=new Tarifa(TipoVehiculo.CARRO,ConceptoTarifa.DIA,new BigDecimal("18000.00"),"COP",entry);

    @BeforeEach void setUp(){
        liquidaciones=mock(LiquidacionRepository.class);detalles=mock(DetalleLiquidacionRepository.class);
        pagos=mock(PagoRepository.class);tarifas=mock(TarifaRepository.class);comprobantes=mock(ComprobanteRepository.class);
        manager=mock(PlatformTransactionManager.class);jdbc=mock(JdbcTemplate.class);
        when(manager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(liquidaciones.findByClaveIdempotencia(key)).thenReturn(Optional.empty());
        when(liquidaciones.findFirstBySesionParqueoIdAndEstadoOrderByCreadoEnDesc(eq(sessionId),eq(EstadoLiquidacion.PENDIENTE))).thenReturn(Optional.empty());
        when(tarifas.findByTipoVehiculoAndConceptoAndActivaTrue(TipoVehiculo.CARRO,ConceptoTarifa.HORA)).thenReturn(Optional.of(hourly));
        when(tarifas.findByTipoVehiculoAndConceptoAndActivaTrue(TipoVehiculo.CARRO,ConceptoTarifa.DIA)).thenReturn(Optional.of(daily));
        when(liquidaciones.saveAndFlush(any(Liquidacion.class))).thenAnswer(inv->{Liquidacion l=inv.getArgument(0);ReflectionTestUtils.setField(l,"id",UUID.randomUUID());return l;});
        when(pagos.saveAndFlush(any(Pago.class))).thenAnswer(inv->{Pago p=inv.getArgument(0);when(pagos.findFirstByLiquidacion_IdAndEstado(p.getLiquidacion().getId(),EstadoPago.PENDIENTE)).thenReturn(Optional.of(p));return p;});
        when(detalles.saveAndFlush(any(DetalleLiquidacion.class))).thenAnswer(inv->inv.getArgument(0));
        service=new LiquidacionService(liquidaciones,detalles,pagos,tarifas,comprobantes,calculator,hash,jdbc,manager);
    }

    @Test void createsDistinctLiquidationPaymentAndTariffDetail(){
        var result=service.crear(key,request(entry.plusSeconds(130*60L)),operatorId);
        assertThat(result.created()).isTrue();
        assertThat(result.liquidacion().total()).isEqualByComparingTo("18000.00");
        assertThat(result.liquidacion().pago().monto()).isEqualByComparingTo("18000.00");
        verify(detalles).saveAndFlush(any(DetalleLiquidacion.class));
        verify(pagos).saveAndFlush(any(Pago.class));
    }

    @Test void freeLiquidationHasReceiptAndNoPayment(){
        when(jdbc.queryForObject("select nextval('seq_comprobante')",Long.class)).thenReturn(1L);
        var result=service.crear(key,request(entry.plusSeconds(8*60L)),operatorId);
        assertThat(result.liquidacion().estado()).isEqualTo(EstadoLiquidacion.GRATUITA);
        assertThat(result.liquidacion().total()).isEqualByComparingTo("0.00");
        assertThat(result.liquidacion().pago()).isNull();
        verify(pagos,never()).saveAndFlush(any(Pago.class));
        verify(comprobantes).saveAndFlush(any(Comprobante.class));
    }

    @Test void sameKeyWithDifferentRequestHashConflicts(){
        var request=request(entry.plusSeconds(130*60L));
        Liquidacion existing=liquidation(key,"a".repeat(64),request.fechaCorte());
        when(liquidaciones.findByClaveIdempotencia(key)).thenReturn(Optional.of(existing));
        assertThatThrownBy(()->service.crear(key,request,operatorId)).isInstanceOf(ConflictException.class);
        verify(tarifas,never()).findByTipoVehiculoAndConceptoAndActivaTrue(any(),any());
    }

    @Test void activePendingLiquidationPreventsDuplicate(){
        var request=request(entry.plusSeconds(130*60L));
        Liquidacion existing=liquidation(UUID.randomUUID(),hash.calcular(request),request.fechaCorte());
        when(liquidaciones.findFirstBySesionParqueoIdAndEstadoOrderByCreadoEnDesc(sessionId,EstadoLiquidacion.PENDIENTE)).thenReturn(Optional.of(existing));
        var result=service.crear(key,request,operatorId);
        assertThat(result.created()).isFalse();
        verify(liquidaciones,never()).saveAndFlush(any(Liquidacion.class));
    }

    private Liquidacion liquidation(UUID key,String requestHash,Instant cutoff){
        Liquidacion l=new Liquidacion(sessionId,"PF-2026-000001","ABC123",TipoVehiculo.CARRO,entry,cutoff,130,(short)15,115,new BigDecimal("12000.00"),key,requestHash,operatorId);
        ReflectionTestUtils.setField(l,"id",UUID.randomUUID());return l;
    }
    private CrearLiquidacionRequest request(Instant cutoff){return new CrearLiquidacionRequest(sessionId,"PF-2026-000001",TipoVehiculo.CARRO,"ABC123",entry,cutoff);}
}
