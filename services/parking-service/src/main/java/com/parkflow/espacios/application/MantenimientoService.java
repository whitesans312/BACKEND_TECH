package com.parkflow.espacios.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parkflow.espacios.api.MantenimientoRequest;
import com.parkflow.espacios.api.MantenimientoResponse;
import com.parkflow.espacios.domain.Espacio;
import com.parkflow.espacios.infrastructure.EspacioRepository;
import com.parkflow.shared.error.ConflictException;
import com.parkflow.shared.error.ResourceNotFoundException;

/**
 * Mantenimiento de espacios (tabla mantenimientos_espacio).
 * Mientras hay un mantenimiento EN_PROCESO el espacio queda en estado MANTENIMIENTO,
 * por lo que no aparece disponible y se rechazan entradas, reservas y check-in sobre él.
 */
@Service
public class MantenimientoService {

    private static final String SELECT_BASE = """
            SELECT m.id, m.espacio_id, e.codigo AS codigo_espacio, m.motivo, m.descripcion, m.estado,
                   m.operador_responsable_id, m.iniciado_en, m.finalizado_en
              FROM mantenimientos_espacio m
              JOIN espacios e ON e.id = m.espacio_id
            """;

    private final EspacioRepository espacios;
    private final JdbcTemplate jdbc;

    public MantenimientoService(EspacioRepository espacios, JdbcTemplate jdbc) {
        this.espacios = espacios;
        this.jdbc = jdbc;
    }

    @Transactional
    public MantenimientoResponse iniciar(UUID espacioId, UUID usuarioId, MantenimientoRequest request) {
        Espacio espacio = bloquearEspacio(espacioId);
        if (espacio.isEnMantenimiento()) {
            throw new ConflictException("El espacio ya está en mantenimiento.");
        }
        if (!espacio.isHabilitado()) {
            throw new ConflictException("El espacio está deshabilitado; restáurelo antes de enviarlo a mantenimiento.");
        }
        if (espacios.existsActiveSession(espacioId)) {
            throw new ConflictException(
                    "El espacio tiene un vehículo activo; trasládelo o registre su salida antes del mantenimiento.");
        }
        if (espacios.existsCurrentActiveReservation(espacioId, Instant.now())) {
            throw new ConflictException("El espacio tiene una reserva vigente en este horario.");
        }
        UUID id;
        try {
            id = jdbc.queryForObject("""
                    INSERT INTO mantenimientos_espacio(espacio_id, operador_responsable_id, administrador_id, motivo, descripcion)
                    VALUES (?, ?, ?, ?, ?)
                    RETURNING id
                    """, UUID.class, espacioId, usuarioId, usuarioId, request.motivo().name(), request.descripcion().trim());
        } catch (DataIntegrityViolationException duplicate) {
            throw new ConflictException("El espacio ya tiene un mantenimiento en proceso.");
        }
        espacio.iniciarMantenimiento();
        espacios.saveAndFlush(espacio);
        return buscar(id);
    }

    @Transactional
    public MantenimientoResponse finalizar(UUID espacioId) {
        return cerrar(espacioId, "FINALIZADO");
    }

    @Transactional
    public MantenimientoResponse cancelar(UUID espacioId) {
        return cerrar(espacioId, "CANCELADO");
    }

    @Transactional(readOnly = true)
    public List<MantenimientoResponse> historial(UUID espacioId) {
        if (!espacios.existsById(espacioId)) {
            throw new ResourceNotFoundException("No existe el espacio solicitado.");
        }
        return jdbc.query(SELECT_BASE + " WHERE m.espacio_id = ? ORDER BY m.iniciado_en DESC",
                MantenimientoService::map, espacioId);
    }

    private MantenimientoResponse cerrar(UUID espacioId, String estadoFinal) {
        Espacio espacio = bloquearEspacio(espacioId);
        List<UUID> enProceso = jdbc.queryForList(
                "SELECT id FROM mantenimientos_espacio WHERE espacio_id = ? AND estado = 'EN_PROCESO'",
                UUID.class, espacioId);
        if (enProceso.isEmpty()) {
            throw new ConflictException("El espacio no tiene un mantenimiento en proceso.");
        }
        UUID id = enProceso.get(0);
        jdbc.update("UPDATE mantenimientos_espacio SET estado = ?, finalizado_en = now() WHERE id = ?", estadoFinal, id);
        espacio.terminarMantenimiento();
        espacios.saveAndFlush(espacio);
        return buscar(id);
    }

    private Espacio bloquearEspacio(UUID espacioId) {
        return espacios.findByIdForUpdate(espacioId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el espacio solicitado."));
    }

    private MantenimientoResponse buscar(UUID id) {
        return jdbc.queryForObject(SELECT_BASE + " WHERE m.id = ?", MantenimientoService::map, id);
    }

    private static MantenimientoResponse map(ResultSet rs, int rowNum) throws SQLException {
        return new MantenimientoResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("espacio_id", UUID.class),
                rs.getString("codigo_espacio"),
                rs.getString("motivo"),
                rs.getString("descripcion"),
                rs.getString("estado"),
                rs.getObject("operador_responsable_id", UUID.class),
                toInstant(rs.getObject("iniciado_en", OffsetDateTime.class)),
                toInstant(rs.getObject("finalizado_en", OffsetDateTime.class)));
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}