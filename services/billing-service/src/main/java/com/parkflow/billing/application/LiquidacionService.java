package com.parkflow.billing.application;

import com.parkflow.billing.api.ComprobanteResponse;
import com.parkflow.billing.api.CancelarLiquidacionRequest;
import com.parkflow.billing.api.CrearLiquidacionRequest;
import com.parkflow.billing.api.LiquidacionResponse;
import com.parkflow.billing.api.PagoResponse;
import com.parkflow.billing.domain.ConceptoTarifa;
import com.parkflow.billing.domain.Comprobante;
import com.parkflow.billing.domain.DetalleLiquidacion;
import com.parkflow.billing.domain.EstadoLiquidacion;
import com.parkflow.billing.domain.Liquidacion;
import com.parkflow.billing.domain.Pago;
import com.parkflow.billing.domain.Tarifa;
import com.parkflow.billing.domain.TipoComprobante;
import com.parkflow.billing.domain.TipoVehiculo;
import com.parkflow.billing.infrastructure.ComprobanteRepository;
import com.parkflow.billing.infrastructure.DetalleLiquidacionRepository;
import com.parkflow.billing.infrastructure.LiquidacionRepository;
import com.parkflow.billing.infrastructure.PagoRepository;
import com.parkflow.billing.infrastructure.TarifaRepository;
import com.parkflow.billing.shared.ConflictException;
import com.parkflow.billing.shared.ResourceNotFoundException;
import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LiquidacionService {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private final LiquidacionRepository liquidaciones;
    private final DetalleLiquidacionRepository detalles;
    private final PagoRepository pagos;
    private final TarifaRepository tarifas;
    private final ComprobanteRepository comprobantes;
    private final CalculadoraLiquidacion calculadora;
    private final HashSolicitud hashSolicitud;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final TransactionTemplate transactions;

    @Autowired public LiquidacionService(LiquidacionRepository liquidaciones, DetalleLiquidacionRepository detalles,
                              PagoRepository pagos, TarifaRepository tarifas, ComprobanteRepository comprobantes,
                              CalculadoraLiquidacion calculadora, HashSolicitud hashSolicitud, JdbcTemplate jdbc,
                              PlatformTransactionManager transactionManager) {
        this(liquidaciones, detalles, pagos, tarifas, comprobantes, calculadora, hashSolicitud, jdbc,
                transactionManager, Clock.systemUTC());
    }

    LiquidacionService(LiquidacionRepository liquidaciones, DetalleLiquidacionRepository detalles,
                       PagoRepository pagos, TarifaRepository tarifas, ComprobanteRepository comprobantes,
                       CalculadoraLiquidacion calculadora, HashSolicitud hashSolicitud, JdbcTemplate jdbc,
                       PlatformTransactionManager transactionManager, Clock clock) {
        this.liquidaciones=liquidaciones; this.detalles=detalles; this.pagos=pagos; this.tarifas=tarifas;
        this.comprobantes=comprobantes; this.calculadora=calculadora; this.hashSolicitud=hashSolicitud;
        this.jdbc=jdbc; this.clock=clock; this.transactions=new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @org.springframework.transaction.annotation.Transactional
    public LiquidacionResult crear(UUID key, CrearLiquidacionRequest request, UUID operatorId) {
        String hash=hashSolicitud.calcular(request);
        Optional<Liquidacion> byKey=liquidaciones.findByClaveIdempotencia(key);
        if (byKey.isPresent()) return existing(byKey.get(), hash, false);
        Optional<Liquidacion> active=liquidaciones.findFirstBySesionParqueoIdAndEstadoOrderByCreadoEnDesc(request.sesionId(), EstadoLiquidacion.PENDIENTE);
        if (active.isPresent()) return existing(active.get(), hash, false);
        try {
            Liquidacion created=transactions.execute(status -> createLocal(key, hash, request, operatorId));
            return new LiquidacionResult(response(created), true);
        } catch (DataIntegrityViolationException race) {
            Optional<Liquidacion> retry=liquidaciones.findByClaveIdempotencia(key)
                    .or(() -> liquidaciones.findFirstBySesionParqueoIdAndEstadoOrderByCreadoEnDesc(request.sesionId(), EstadoLiquidacion.PENDIENTE));
            if (retry.isPresent()) return existing(retry.get(), hash, false);
            throw race;
        }
    }

    private Liquidacion createLocal(UUID key, String hash, CrearLiquidacionRequest request, UUID operatorId) {
        Tarifa hourly=tarifas.findByTipoVehiculoAndConceptoAndActivaTrue(request.tipoVehiculo(), ConceptoTarifa.HORA)
                .orElseThrow(() -> new ConflictException("No hay tarifa horaria vigente."));
        Tarifa daily=tarifas.findByTipoVehiculoAndConceptoAndActivaTrue(request.tipoVehiculo(), ConceptoTarifa.DIA)
                .orElseThrow(() -> new ConflictException("No hay tarifa diaria vigente."));
        CalculoLiquidacion calc=calculadora.calcular(hourly, daily, request.fechaEntrada(), request.fechaCorte());
        Liquidacion l=new Liquidacion(request.sesionId(), request.numeroTicket(), request.placa(), request.tipoVehiculo(),
                request.fechaEntrada(), request.fechaCorte(), calc.minutos(), (short)calculadora.minutosGracia(), calc.minutosFacturables(),
                calc.total(), key, hash, operatorId);
        l=liquidaciones.saveAndFlush(l);
        detalles.saveAndFlush(new DetalleLiquidacion(l.getId(), calc.tarifaAplicada().getId(), calc.concepto(),
                calc.concepto().equals("TIEMPO_GRATIS") ? "Permanencia dentro del periodo de gracia" : "Tarifa vigente aplicada",
                java.math.BigDecimal.valueOf(calc.unidades()), calc.tarifaAplicada().getValor(), calc.total()));
        if (l.getEstado() == EstadoLiquidacion.GRATUITA) {
            comprobantes.saveAndFlush(new Comprobante(l, null, TipoComprobante.SALIDA_GRATUITA, nextReceiptNumber(), clock.instant()));
        } else {
            pagos.saveAndFlush(new Pago(l, key, operatorId));
        }
        return l;
    }

    private String nextReceiptNumber() {
        long sequence=jdbc.queryForObject("select nextval('seq_comprobante')", Long.class);
        return "PF-CMP-%04d-%06d".formatted(Year.from(clock.instant().atZone(BOGOTA)).getValue(), sequence);
    }

    private LiquidacionResult existing(Liquidacion l, String hash, boolean created) {
        if (!l.getHashSolicitud().equals(hash)) throw new ConflictException("La clave idempotente ya fue utilizada con contenido diferente.");
        return new LiquidacionResult(response(l), created);
    }

    private LiquidacionResponse response(Liquidacion l) {
        PagoResponse payment=pagos.findFirstByLiquidacion_IdAndEstado(l.getId(), com.parkflow.billing.domain.EstadoPago.PENDIENTE)
                .or(() -> pagos.findFirstByLiquidacion_IdAndEstado(l.getId(), com.parkflow.billing.domain.EstadoPago.APROBADO))
                .or(() -> pagos.findFirstByLiquidacion_IdAndEstado(l.getId(), com.parkflow.billing.domain.EstadoPago.RECHAZADO))
                .map(p -> {
                    ComprobanteResponse receipt=comprobantes.findByPago_Id(p.getId()).map(ComprobanteResponse::from).orElse(null);
                    return PagoResponse.from(p, receipt);
                }).orElse(null);
        ComprobanteResponse receipt=comprobantes.findByLiquidacion_Id(l.getId()).map(ComprobanteResponse::from).orElse(null);
        return LiquidacionResponse.from(l, payment, receipt);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public LiquidacionResponse porClave(UUID key) {
        Liquidacion l=liquidaciones.findByClaveIdempotencia(key)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una liquidación para esa clave."));
        return response(l);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public LiquidacionResponse porId(UUID id) {
        Liquidacion l=liquidaciones.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la liquidación solicitada."));
        return response(l);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public List<LiquidacionResponse> porSesion(UUID sessionId) {
        return liquidaciones.findBySesionParqueoIdOrderByCreadoEnDesc(sessionId).stream().map(this::response).toList();
    }

    @org.springframework.transaction.annotation.Transactional
    public LiquidacionResponse cancelar(UUID id, CancelarLiquidacionRequest request, UUID operatorId) {
        Liquidacion l=liquidaciones.lockById(id)
                .orElseThrow(() -> new com.parkflow.billing.shared.ResourceNotFoundException("No existe la liquidación solicitada."));
        String reason=request.motivo().trim();
        if (l.getEstado()==EstadoLiquidacion.CANCELADA) {
            if (operatorId.equals(l.getCanceladaPor()) && reason.equals(l.getMotivoCierre())) return response(l);
            throw new ConflictException("La liquidación ya fue cancelada con otra resolución.");
        }
        if (l.getEstado()!=EstadoLiquidacion.PENDIENTE) throw new ConflictException("Solo se puede cancelar una liquidación pendiente.");
        pagos.findFirstByLiquidacion_IdAndEstado(l.getId(),com.parkflow.billing.domain.EstadoPago.PENDIENTE).ifPresent(p -> {
            p.reject(operatorId, "Liquidación cancelada: " + reason, clock.instant());
            pagos.saveAndFlush(p);
        });
        l.cancelar(operatorId,reason,clock.instant());
        liquidaciones.saveAndFlush(l);
        return response(l);
    }
}
