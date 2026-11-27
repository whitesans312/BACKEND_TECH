package com.parkflow.reservas.application;

import com.parkflow.espacios.domain.Espacio;
import com.parkflow.espacios.infrastructure.EspacioRepository;
import com.parkflow.reservas.api.CancelarReservaRequest;
import com.parkflow.reservas.api.CrearReservaRequest;
import com.parkflow.reservas.api.ReservaCheckInResponse;
import com.parkflow.reservas.api.ReservaResponse;
import com.parkflow.reservas.domain.EstadoReserva;
import com.parkflow.reservas.domain.Reserva;
import com.parkflow.reservas.infrastructure.ReservaRepository;
import com.parkflow.sesiones.api.SesionResponse;
import com.parkflow.sesiones.domain.EstadoSesion;
import com.parkflow.sesiones.domain.SesionParqueo;
import com.parkflow.sesiones.infrastructure.SesionParqueoRepository;
import com.parkflow.shared.error.ConflictException;
import com.parkflow.shared.error.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaService {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final ReservaRepository reservas;
    private final EspacioRepository espacios;
    private final SesionParqueoRepository sesiones;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Autowired
    public ReservaService(ReservaRepository reservas, EspacioRepository espacios,
                          SesionParqueoRepository sesiones, JdbcTemplate jdbc) {
        this(reservas, espacios, sesiones, jdbc, Clock.systemUTC());
    }

    ReservaService(ReservaRepository reservas, EspacioRepository espacios,
                   SesionParqueoRepository sesiones, JdbcTemplate jdbc, Clock clock) {
        this.reservas = reservas;
        this.espacios = espacios;
        this.sesiones = sesiones;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional
    public ReservaResponse crear(CrearReservaRequest request, UUID operadorId, UUID operationId) {
        String placa = normalizarPlaca(request.placa());
        var existing = reservas.findByClaveIdempotencia(operationId);
        if (existing.isPresent()) {
            Reserva prior = existing.get();
            if (!prior.getPlaca().equals(placa)
                    || prior.getTipoVehiculo() != request.tipoVehiculo()
                    || !prior.getEspacio().getId().equals(request.espacioId())
                    || !prior.getFechaInicio().equals(request.fechaInicio())
                    || !prior.getFechaFin().equals(request.fechaFin())) {
                throw new ConflictException("La clave de reserva ya fue usada con datos diferentes.");
            }
            return ReservaResponse.from(prior);
        }

        Instant now = clock.instant();
        if (!request.fechaFin().isAfter(request.fechaInicio())) {
            throw new IllegalArgumentException("La fecha de fin de la reserva debe ser posterior a la de inicio.");
        }
        if (!request.fechaFin().isAfter(now)) {
            throw new IllegalArgumentException("La reserva debe finalizar en un instante futuro.");
        }

        Espacio espacio = espacios.findByIdForUpdate(request.espacioId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el espacio solicitado."));
        if (espacio.getTipo() != request.tipoVehiculo()) {
            throw new ConflictException("El tipo de vehículo no es compatible con el espacio.");
        }
        if (espacio.isEnMantenimiento()) {
            throw new ConflictException("El espacio está en mantenimiento.");
        }
        if (!espacio.isHabilitado() || !espacio.getZona().isActiva() || !espacio.getZona().getPiso().isHabilitado()) {
            throw new ConflictException("El espacio, la zona o el piso no están habilitados.");
        }

        List<Reserva> expired = reservas.findExpiredActiveForSpaceOrPlate(espacio.getId(), placa, now);
        if (!expired.isEmpty()) {
            expired.forEach(item -> item.expirar(now));
            reservas.saveAllAndFlush(expired);
        }

        if (sesiones.existsByPlacaActualAndEstado(placa, EstadoSesion.ACTIVA)) {
            throw new ConflictException("El vehículo ya tiene una sesión activa en el parqueadero.");
        }
        if (reservas.existsByPlacaAndEstado(placa, EstadoReserva.ACTIVA)) {
            throw new ConflictException("La placa ya tiene una reserva activa.");
        }
        if (reservas.existsOverlappingActiveReservation(espacio.getId(), request.fechaInicio(), request.fechaFin(), now)) {
            throw new ConflictException("El espacio ya tiene una reserva activa que se cruza con ese horario.");
        }
        if (!request.fechaInicio().isAfter(now) && espacios.existsActiveSession(espacio.getId())) {
            throw new ConflictException("El espacio está ocupado actualmente y no admite reserva inmediata.");
        }

        Long sequence = jdbc.queryForObject("select nextval('seq_reserva')", Long.class);
        String code = "PF-RES-%04d-%06d".formatted(Year.from(now.atZone(BOGOTA)).getValue(), sequence);
        String obs = request.observacion() == null ? null : request.observacion().trim();
        Reserva created = new Reserva(code, espacio, placa, request.tipoVehiculo(),
                request.fechaInicio(), request.fechaFin(), operationId, operadorId, obs);
        try {
            return ReservaResponse.from(reservas.saveAndFlush(created));
        } catch (DataIntegrityViolationException duplicate) {
            throw new ConflictException("La reserva entró en conflicto; reintente con la misma clave de idempotencia.");
        }
    }

    @Transactional
    public List<ReservaResponse> listar(EstadoReserva estado, String placa) {
        Instant now = clock.instant();
        reservas.expirePastDue(now);
        String normalizedPlate = (placa == null || placa.isBlank()) ? null : normalizarPlaca(placa);
        return reservas.findAllFiltered(estado, normalizedPlate).stream().map(ReservaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ReservaResponse obtener(UUID id) {
        return reservas.findDetailedById(id)
                .map(ReservaResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva solicitada."));
    }

    @Transactional
    public ReservaCheckInResponse checkIn(UUID id, UUID operadorId, UUID operationId) {
        var existingSession = sesiones.findByClaveIdempotencia(operationId);
        if (existingSession.isPresent()) {
            SesionParqueo priorSession = existingSession.get();
            Reserva priorReserva = reservas.findDetailedById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva solicitada."));
            if (priorSession.getId().equals(priorReserva.getSesionId())) {
                return new ReservaCheckInResponse(ReservaResponse.from(priorReserva), SesionResponse.from(priorSession));
            }
            throw new ConflictException("La clave de ingreso ya fue usada con otra reserva.");
        }

        Reserva reserva = reservas.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva solicitada."));
        Instant now = clock.instant();

        if (reserva.getEstado() == EstadoReserva.CONVERTIDA) {
            throw new ConflictException("La reserva ya fue convertida en sesión con otra operación.");
        }
        if (reserva.getEstado() == EstadoReserva.CANCELADA || reserva.getEstado() == EstadoReserva.EXPIRADA) {
            throw new ConflictException("La reserva ya no se encuentra activa.");
        }
        if (!reserva.getFechaFin().isAfter(now)) {
            reserva.expirar(now);
            reservas.saveAndFlush(reserva);
            throw new ConflictException("La reserva expiró antes de realizar el check-in.");
        }

        Espacio espacio = espacios.findByIdForUpdate(reserva.getEspacio().getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el espacio reservado."));
        if (espacio.isEnMantenimiento()) {
            throw new ConflictException("El espacio está en mantenimiento.");
        }
        if (!espacio.isHabilitado() || !espacio.getZona().isActiva() || !espacio.getZona().getPiso().isHabilitado()) {
            throw new ConflictException("El espacio, la zona o el piso no están habilitados.");
        }
        if (espacios.existsActiveSession(espacio.getId())) {
            throw new ConflictException("El espacio aún tiene un vehículo activo.");
        }
        if (sesiones.existsByPlacaActualAndEstado(reserva.getPlaca(), EstadoSesion.ACTIVA)) {
            throw new ConflictException("El vehículo ya tiene una sesión activa.");
        }

        Long sequence = jdbc.queryForObject("select nextval('seq_ticket')", Long.class);
        String ticket = "PF-%04d-%06d".formatted(Year.from(now.atZone(BOGOTA)).getValue(), sequence);
        SesionParqueo session = new SesionParqueo(ticket, reserva.getPlaca(), reserva.getTipoVehiculo(), espacio, operadorId);
        session.setClaveIdempotencia(operationId);
        try {
            SesionParqueo savedSession = sesiones.saveAndFlush(session);
            reserva.convertir(savedSession.getId(), operadorId, now);
            Reserva savedReserva = reservas.saveAndFlush(reserva);
            return new ReservaCheckInResponse(ReservaResponse.from(savedReserva), SesionResponse.from(savedSession));
        } catch (DataIntegrityViolationException duplicate) {
            throw new ConflictException("El check-in pudo procesarse; reintente con la misma clave de idempotencia.");
        }
    }

    @Transactional
    public ReservaResponse cancelar(UUID id, UUID operadorId, CancelarReservaRequest request) {
        Reserva reserva = reservas.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva solicitada."));
        String motivo = request == null || request.motivo() == null || request.motivo().isBlank()
                ? null : request.motivo().trim();
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            if (Objects.equals(motivo, reserva.getMotivoCancelacion())) {
                return ReservaResponse.from(reserva);
            }
            throw new ConflictException("La reserva ya fue cancelada con otro motivo.");
        }
        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new ConflictException("Solo se pueden cancelar reservas activas.");
        }
        reserva.cancelar(operadorId, motivo, clock.instant());
        return ReservaResponse.from(reservas.saveAndFlush(reserva));
    }

    private String normalizarPlaca(String plate) {
        String normalized = plate.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9]{4,10}")) {
            throw new IllegalArgumentException("La placa debe contener entre 4 y 10 letras o números.");
        }
        return normalized;
    }
}