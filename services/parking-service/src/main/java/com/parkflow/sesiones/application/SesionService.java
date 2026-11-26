package com.parkflow.sesiones.application;

import com.parkflow.espacios.domain.Espacio;
import com.parkflow.espacios.domain.TipoEspacio;
import com.parkflow.espacios.infrastructure.EspacioRepository;
import com.parkflow.shared.error.ConflictException;
import com.parkflow.shared.error.ResourceNotFoundException;
import com.parkflow.sesiones.api.CorreccionPlacaRequest;
import com.parkflow.sesiones.api.EntradaRequest;
import com.parkflow.sesiones.api.SalidaResponse;
import com.parkflow.sesiones.api.SesionResponse;
import com.parkflow.sesiones.domain.EstadoSesion;
import com.parkflow.sesiones.domain.SesionParqueo;
import com.parkflow.sesiones.infrastructure.SesionParqueoRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class SesionService {
    private final SesionParqueoRepository sesiones;
    private final EspacioRepository espacios;
    private final JdbcTemplate jdbc;
    private final RestClient billing;
    private final TransactionTemplate writeTransactions;
    private final TransactionTemplate readTransactions;

    @Autowired
    public SesionService(SesionParqueoRepository sesiones, EspacioRepository espacios, JdbcTemplate jdbc,
                         @Value("${parkflow.billing.base-url:http://127.0.0.1:18082}") String billingUrl,
                         PlatformTransactionManager transactionManager) {
        this(sesiones, espacios, jdbc, buildBillingClient(RestClient.builder().baseUrl(billingUrl)), transactionManager);
    }

    SesionService(SesionParqueoRepository sesiones, EspacioRepository espacios, JdbcTemplate jdbc, String billingUrl) {
        this(sesiones, espacios, jdbc, billingUrl, null);
    }

    SesionService(SesionParqueoRepository sesiones, EspacioRepository espacios, JdbcTemplate jdbc,
                  RestClient.Builder billingBuilder, PlatformTransactionManager transactionManager) {
        this(sesiones, espacios, jdbc, billingBuilder.build(), transactionManager);
    }

    private SesionService(SesionParqueoRepository sesiones, EspacioRepository espacios, JdbcTemplate jdbc,
                          RestClient billingClient, PlatformTransactionManager transactionManager) {
        this.sesiones = sesiones;
        this.espacios = espacios;
        this.jdbc = jdbc;
        this.billing = billingClient;
        if (transactionManager != null) {
            this.writeTransactions = new TransactionTemplate(transactionManager);
            this.readTransactions = new TransactionTemplate(transactionManager);
            this.readTransactions.setReadOnly(true);
            this.readTransactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        } else {
            this.writeTransactions = null;
            this.readTransactions = null;
        }
    }

    private static RestClient buildBillingClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return builder.requestFactory(requestFactory).build();
    }

    @Transactional
    public SesionResponse registrarEntrada(EntradaRequest request, UUID operadorId, UUID operationId) {
        String placa = request.placa().trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        var existing = sesiones.findByClaveIdempotencia(operationId);
        if (existing.isPresent()) {
            SesionParqueo prior = existing.get();
            if (!prior.getPlacaActual().equals(placa) || prior.getTipoVehiculo() != request.tipoVehiculo()
                    || !prior.getEspacio().getId().equals(request.espacioId())) {
                throw new ConflictException("La clave de ingreso ya fue usada con datos diferentes.");
            }
            return SesionResponse.from(prior);
        }
        if (sesiones.existsByPlacaActualAndEstado(placa, EstadoSesion.ACTIVA)) {
            throw new ConflictException("El vehículo ya tiene una sesión activa.");
        }
        Espacio espacio = espacios.findByIdForUpdate(request.espacioId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el espacio solicitado."));
        if (espacio.getTipo() != request.tipoVehiculo()) {
            throw new ConflictException("El tipo de vehículo no es compatible con el espacio.");
        }
        if (!espacio.isHabilitado() || !espacio.getZona().isActiva() || !espacio.getZona().getPiso().isHabilitado()) {
            throw new ConflictException("El espacio, la zona o el piso no están habilitados.");
        }
        if (espacios.existsActiveSession(espacio.getId())) {
            throw new ConflictException("El espacio ya tiene un vehículo activo.");
        }
        if (espacios.existsCurrentActiveReservation(espacio.getId(), Instant.now())) {
            throw new ConflictException("El espacio tiene una reserva vigente en este horario.");
        }
        Long sequence = jdbc.queryForObject("select nextval('seq_ticket')", Long.class);
        String ticket = "PF-%04d-%06d".formatted(Year.now().getValue(), sequence);
        SesionParqueo created = new SesionParqueo(ticket, placa, request.tipoVehiculo(), espacio, operadorId);
        created.setClaveIdempotencia(operationId);
        try {
            return SesionResponse.from(sesiones.saveAndFlush(created));
        } catch (org.springframework.dao.DataIntegrityViolationException duplicate) {
            throw new ConflictException("El ingreso pudo procesarse; reintente con la misma clave de idempotencia para recuperar el tiquete.");
        }
    }

    @Transactional(readOnly = true)
    public List<SesionResponse> findAll(boolean activeOnly) {
        return (activeOnly ? sesiones.findByEstadoOrderByFechaEntradaDesc(EstadoSesion.ACTIVA)
                : sesiones.findAllByOrderByFechaEntradaDesc())
                .stream().map(SesionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SesionResponse findByTicket(String ticket) {
        return SesionResponse.from(sesiones.findByNumeroTicket(ticket)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el ticket solicitado.")));
    }

    @Transactional(readOnly = true)
    public List<SesionResponse> buscarPorPlaca(String placa) {
        String normalized = normalizarPlaca(placa);
        return sesiones.findByPlacaActualOrderByFechaEntradaDesc(normalized).stream().map(SesionResponse::from).toList();
    }

    @Transactional
    public SesionResponse corregirPlaca(String ticket, UUID operationId, UUID operatorId, CorreccionPlacaRequest request) {
        SesionParqueo session = sesiones.findByNumeroTicketForUpdate(ticket)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el ticket solicitado."));
        String newPlate = normalizarPlaca(request.placa());
        String oldPlate = session.getPlacaActual();
        int inserted = jdbc.update("""
                INSERT INTO eventos_operativos(sesion_id,operador_id,tipo_evento,motivo,datos_anteriores,datos_nuevos,clave_idempotencia)
                VALUES (?,?,'CORRECCION_PLACA',?,jsonb_build_object('placa',?),jsonb_build_object('placa',?),?)
                ON CONFLICT (clave_idempotencia) DO NOTHING
                """, session.getId(), operatorId, request.motivo().trim(), oldPlate, newPlate, operationId);
        if (inserted == 0) {
            Boolean same = jdbc.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM eventos_operativos WHERE clave_idempotencia=? AND sesion_id=? AND tipo_evento='CORRECCION_PLACA')",
                    Boolean.class, operationId, session.getId());
            if (!Boolean.TRUE.equals(same)) {
                throw new ConflictException("La clave de idempotencia ya fue usada por otra operación.");
            }
            return SesionResponse.from(session);
        }
        if (sesiones.existsByPlacaActualAndEstado(newPlate, EstadoSesion.ACTIVA) && !newPlate.equals(oldPlate)) {
            throw new ConflictException("La placa ya está asociada a otra sesión activa.");
        }
        session.corregirPlaca(newPlate);
        return SesionResponse.from(sesiones.saveAndFlush(session));
    }

    private String normalizarPlaca(String plate) {
        String normalized = plate.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9]{4,10}")) {
            throw new IllegalArgumentException("La placa debe contener entre 4 y 10 letras o números.");
        }
        return normalized;
    }

    /**
     * Orquesta la salida sin mantener bloqueos ni transacciones abiertas sobre PostgreSQL
     * durante la llamada HTTP remota hacia billing-service (HT-05).
     */
    public SalidaResponse solicitarSalida(String ticket, UUID operationId, Jwt jwt) {
        SalidaSnapshot snapshot = executeReadOnly(() -> {
            SesionParqueo session = sesiones.findByNumeroTicket(ticket)
                    .orElseThrow(() -> new ResourceNotFoundException("No existe el ticket solicitado."));
            if (session.getEstado() == EstadoSesion.CERRADA && !operationId.equals(session.getOperacionCierreId())) {
                throw new ConflictException("La sesión ya fue cerrada con otra operación.");
            }
            if (session.getEstado() != EstadoSesion.ACTIVA && session.getEstado() != EstadoSesion.CERRADA) {
                throw new ConflictException("La sesión no se encuentra activa para registrar salida.");
            }
            return new SalidaSnapshot(
                    session.getId(),
                    session.getNumeroTicket(),
                    session.getTipoVehiculo(),
                    session.getPlacaActual(),
                    session.getFechaEntrada(),
                    session.getEstado(),
                    session.getOperacionCierreId(),
                    SesionResponse.from(session));
        });

        BillingSettlement settlement = resolveOrCreateSettlement(snapshot, operationId, jwt);

        if (!"PAGADA".equals(settlement.estado()) && !"GRATUITA".equals(settlement.estado())) {
            if (snapshot.estado() == EstadoSesion.CERRADA) {
                if (operationId.equals(snapshot.operacionCierreId())) {
                    return new SalidaResponse(snapshot.response(), settlement.id(), settlement.estado(), settlement.total(), true);
                }
                throw new ConflictException("La sesión ya fue cerrada con otra operación.");
            }
            return new SalidaResponse(snapshot.response(), settlement.id(), settlement.estado(), settlement.total(), false);
        }

        UUID operadorId = UUID.fromString(jwt.getSubject());
        return executeWrite(() -> {
            SesionParqueo locked = sesiones.findByNumeroTicketForUpdate(ticket)
                    .orElseThrow(() -> new ResourceNotFoundException("No existe el ticket solicitado."));
            if (locked.getEstado() == EstadoSesion.CERRADA) {
                if (operationId.equals(locked.getOperacionCierreId())) {
                    return new SalidaResponse(SesionResponse.from(locked), settlement.id(), settlement.estado(), settlement.total(), true);
                }
                throw new ConflictException("La sesión ya fue cerrada con otra operación.");
            }
            locked.cerrar(settlement.id(), operationId, operadorId, Instant.now(), settlement.estado());
            sesiones.saveAndFlush(locked);
            return new SalidaResponse(SesionResponse.from(locked), settlement.id(), settlement.estado(), settlement.total(), true);
        });
    }

    private BillingSettlement resolveOrCreateSettlement(SalidaSnapshot snapshot, UUID operationId, Jwt jwt) {
        BillingSettlement settlement;
        try {
            settlement = billing.get()
                    .uri("/internal/v1/liquidaciones/by-idempotency-key/{key}", operationId)
                    .headers(h -> h.setBearerAuth(jwt.getTokenValue()))
                    .retrieve()
                    .body(BillingSettlement.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() != 404) {
                throw new ConflictException("Billing no pudo confirmar el estado de la liquidación; reintente con la misma clave.");
            }
            if (snapshot.estado() != EstadoSesion.ACTIVA) {
                throw new ConflictException("La sesión ya está cerrada con otra operación.");
            }
            var body = new CrearLiquidacion(snapshot.id(), snapshot.numeroTicket(), snapshot.tipoVehiculo().name(),
                    snapshot.placa(), snapshot.fechaEntrada(), Instant.now());
            try {
                settlement = billing.post()
                        .uri("/internal/v1/liquidaciones")
                        .headers(h -> h.setBearerAuth(jwt.getTokenValue()))
                        .header("Idempotency-Key", operationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(BillingSettlement.class);
            } catch (RestClientException remote) {
                throw new ConflictException("Billing no pudo crear o recuperar la liquidación; reintente con la misma clave.");
            }
        } catch (RestClientException e) {
            throw new ConflictException("Billing no pudo confirmar el estado de la liquidación; reintente con la misma clave.");
        }
        if (settlement == null) {
            throw new ConflictException("Billing devolvió una respuesta vacía; reintente con la misma clave.");
        }
        return settlement;
    }

    private <T> T executeReadOnly(Supplier<T> action) {
        return readTransactions == null ? action.get() : readTransactions.execute(status -> action.get());
    }

    private <T> T executeWrite(Supplier<T> action) {
        return writeTransactions == null ? action.get() : writeTransactions.execute(status -> action.get());
    }

    private record SalidaSnapshot(UUID id, String numeroTicket, TipoEspacio tipoVehiculo, String placa,
                                  Instant fechaEntrada, EstadoSesion estado, UUID operacionCierreId,
                                  SesionResponse response) { }

    private record CrearLiquidacion(UUID sesionId, String numeroTicket, String tipoVehiculo, String placa,
                                    Instant fechaEntrada, Instant fechaCorte) { }

    private record BillingSettlement(UUID id, String estado, BigDecimal total) { }
}
