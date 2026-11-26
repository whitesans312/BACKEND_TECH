package com.parkflow.reservas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parkflow.espacios.domain.Espacio;
import com.parkflow.espacios.domain.TipoEspacio;
import com.parkflow.espacios.infrastructure.EspacioRepository;
import com.parkflow.pisos.domain.Piso;
import com.parkflow.reservas.api.CrearReservaRequest;
import com.parkflow.reservas.domain.EstadoReserva;
import com.parkflow.reservas.domain.Reserva;
import com.parkflow.reservas.infrastructure.ReservaRepository;
import com.parkflow.sesiones.domain.EstadoSesion;
import com.parkflow.sesiones.domain.SesionParqueo;
import com.parkflow.sesiones.infrastructure.SesionParqueoRepository;
import com.parkflow.shared.error.ConflictException;
import com.parkflow.zonas.domain.Zona;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-25T18:00:00Z");

    @Mock ReservaRepository reservas;
    @Mock EspacioRepository espacios;
    @Mock SesionParqueoRepository sesiones;
    @Mock JdbcTemplate jdbc;

    ReservaService service;

    @BeforeEach
    void setup() {
        service = new ReservaService(reservas, espacios, sesiones, jdbc, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsFutureReservationUsingIdempotencyKeyAndPessimisticSpaceLock() {
        UUID spaceId = UUID.randomUUID();
        UUID opId = UUID.randomUUID();
        Espacio space = mockSpace(spaceId, TipoEspacio.CARRO, true);
        Instant start = NOW.plusSeconds(3600);
        Instant end = NOW.plusSeconds(10800);

        when(reservas.findByClaveIdempotencia(opId)).thenReturn(Optional.empty());
        when(espacios.findByIdForUpdate(spaceId)).thenReturn(Optional.of(space));
        when(reservas.findExpiredActiveForSpaceOrPlate(spaceId, "ABC123", NOW)).thenReturn(List.of());
        when(sesiones.existsByPlacaActualAndEstado("ABC123", EstadoSesion.ACTIVA)).thenReturn(false);
        when(reservas.existsByPlacaAndEstado("ABC123", EstadoReserva.ACTIVA)).thenReturn(false);
        when(reservas.existsOverlappingActiveReservation(spaceId, start, end, NOW)).thenReturn(false);
        when(jdbc.queryForObject("select nextval('seq_reserva')", Long.class)).thenReturn(7L);
        when(reservas.saveAndFlush(any(Reserva.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.crear(
                new CrearReservaRequest("abc 123", TipoEspacio.CARRO, spaceId, start, end, "Cliente VIP"),
                UUID.randomUUID(), opId);

        assertThat(response.placa()).isEqualTo("ABC123");
        assertThat(response.codigoReserva()).isEqualTo("PF-RES-2026-000007");
        assertThat(response.estado()).isEqualTo(EstadoReserva.ACTIVA);
        verify(espacios).findByIdForUpdate(spaceId);
    }

    @Test
    void returnsSameReservationOnIdempotentRetry() {
        UUID spaceId = UUID.randomUUID();
        UUID opId = UUID.randomUUID();
        Espacio space = mockSpace(spaceId, TipoEspacio.CARRO, true);
        Instant start = NOW.plusSeconds(3600);
        Instant end = NOW.plusSeconds(7200);
        Reserva prior = new Reserva("PF-RES-2026-000001", space, "ABC123", TipoEspacio.CARRO,
                start, end, opId, UUID.randomUUID(), null);
        when(reservas.findByClaveIdempotencia(opId)).thenReturn(Optional.of(prior));

        var response = service.crear(
                new CrearReservaRequest("ABC123", TipoEspacio.CARRO, spaceId, start, end, null),
                UUID.randomUUID(), opId);

        assertThat(response.codigoReserva()).isEqualTo("PF-RES-2026-000001");
        verify(espacios, never()).findByIdForUpdate(any());
        verify(reservas, never()).saveAndFlush(any());
    }

    @Test
    void rejectsOverlappingReservationForSameSpace() {
        UUID spaceId = UUID.randomUUID();
        UUID opId = UUID.randomUUID();
        Espacio space = mockSpace(spaceId, TipoEspacio.CARRO, true);
        Instant start = NOW.plusSeconds(3600);
        Instant end = NOW.plusSeconds(7200);

        when(reservas.findByClaveIdempotencia(opId)).thenReturn(Optional.empty());
        when(espacios.findByIdForUpdate(spaceId)).thenReturn(Optional.of(space));
        when(reservas.findExpiredActiveForSpaceOrPlate(spaceId, "XYZ789", NOW)).thenReturn(List.of());
        when(sesiones.existsByPlacaActualAndEstado("XYZ789", EstadoSesion.ACTIVA)).thenReturn(false);
        when(reservas.existsByPlacaAndEstado("XYZ789", EstadoReserva.ACTIVA)).thenReturn(false);
        when(reservas.existsOverlappingActiveReservation(spaceId, start, end, NOW)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(
                new CrearReservaRequest("XYZ789", TipoEspacio.CARRO, spaceId, start, end, null),
                UUID.randomUUID(), opId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cruza");
        verify(jdbc, never()).queryForObject(anyString(), eq(Long.class));
    }

    @Test
    void convertsActiveReservationToSessionOnCheckIn() {
        UUID reservaId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID checkInOpId = UUID.randomUUID();
        Espacio space = mockSpace(spaceId, TipoEspacio.MOTO, true);
        Reserva reserva = new Reserva("PF-RES-2026-000005", space, "MOT55K", TipoEspacio.MOTO,
                NOW.minusSeconds(300), NOW.plusSeconds(3600), UUID.randomUUID(), UUID.randomUUID(), null);

        when(sesiones.findByClaveIdempotencia(checkInOpId)).thenReturn(Optional.empty());
        when(reservas.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(espacios.findByIdForUpdate(spaceId)).thenReturn(Optional.of(space));
        when(espacios.existsActiveSession(spaceId)).thenReturn(false);
        when(sesiones.existsByPlacaActualAndEstado("MOT55K", EstadoSesion.ACTIVA)).thenReturn(false);
        when(jdbc.queryForObject("select nextval('seq_ticket')", Long.class)).thenReturn(15L);
        when(sesiones.saveAndFlush(any(SesionParqueo.class))).thenAnswer(call -> call.getArgument(0));
        when(reservas.saveAndFlush(any(Reserva.class))).thenAnswer(call -> call.getArgument(0));

        var result = service.checkIn(reservaId, UUID.randomUUID(), checkInOpId);

        assertThat(result.reserva().estado()).isEqualTo(EstadoReserva.CONVERTIDA);
        assertThat(result.sesion().numeroTicket()).isEqualTo("PF-2026-000015");
        assertThat(result.sesion().placa()).isEqualTo("MOT55K");
    }

    private static Espacio mockSpace(UUID spaceId, TipoEspacio tipo, boolean enabled) {
        Espacio space = mock(Espacio.class);
        Zona zone = mock(Zona.class);
        Piso floor = mock(Piso.class);
        lenient().when(space.getId()).thenReturn(spaceId);
        lenient().when(space.getCodigo()).thenReturn("C-01");
        lenient().when(space.getTipo()).thenReturn(tipo);
        lenient().when(space.isHabilitado()).thenReturn(enabled);
        lenient().when(space.getZona()).thenReturn(zone);
        lenient().when(zone.isActiva()).thenReturn(enabled);
        lenient().when(zone.getPiso()).thenReturn(floor);
        lenient().when(floor.isHabilitado()).thenReturn(enabled);
        lenient().when(floor.getCodigo()).thenReturn("P1");
        return space;
    }
}
