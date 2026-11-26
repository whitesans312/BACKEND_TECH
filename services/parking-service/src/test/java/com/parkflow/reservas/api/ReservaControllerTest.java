package com.parkflow.reservas.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkflow.espacios.domain.TipoEspacio;
import com.parkflow.reservas.application.ReservaService;
import com.parkflow.reservas.domain.EstadoReserva;
import com.parkflow.sesiones.api.SesionResponse;
import com.parkflow.sesiones.domain.EstadoSesion;
import com.parkflow.shared.error.ApiExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class ReservaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsListsChecksInAndCancelsReservations() throws Exception {
        UUID id = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID opId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant start = Instant.parse("2026-11-26T14:00:00Z");
        Instant end = Instant.parse("2026-11-26T16:00:00Z");

        Jwt token = Jwt.withTokenValue("test-token").header("alg", "RS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));

        var created = new ReservaResponse(id, "PF-RES-2026-000001", spaceId, "C-01", "P1",
                "ABC123", TipoEspacio.CARRO, start, end, EstadoReserva.ACTIVA, null, "VIP", null, start);
        when(service.crear(any(CrearReservaRequest.class), eq(userId), eq(opId))).thenReturn(created);
        when(service.listar(EstadoReserva.ACTIVA, null)).thenReturn(List.of(created));

        mockMvc.perform(post("/api/v1/reservas")
                        .header("Idempotency-Key", opId.toString())
                        .contentType("application/json")
                        .content("""
                                {"placa":"ABC123","tipoVehiculo":"CARRO","espacioId":"%s",
                                "fechaInicio":"2026-11-26T14:00:00Z","fechaFin":"2026-11-26T16:00:00Z","observacion":"VIP"}
                                """.formatted(spaceId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/reservas/" + id))
                .andExpect(jsonPath("$.codigoReserva").value("PF-RES-2026-000001"));

        mockMvc.perform(get("/api/v1/reservas").queryParam("estado", "ACTIVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placa").value("ABC123"));

        var converted = new ReservaResponse(id, "PF-RES-2026-000001", spaceId, "C-01", "P1",
                "ABC123", TipoEspacio.CARRO, start, end, EstadoReserva.CONVERTIDA, UUID.randomUUID(), "VIP", null, start);
        var session = new SesionResponse(converted.sesionId(), "PF-2026-000010", "ABC123", TipoEspacio.CARRO,
                spaceId, "C-01", EstadoSesion.ACTIVA, start, null, userId);
        when(service.checkIn(id, userId, opId)).thenReturn(new ReservaCheckInResponse(converted, session));

        mockMvc.perform(post("/api/v1/reservas/{id}/check-in", id)
                        .header("Idempotency-Key", opId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserva.estado").value("CONVERTIDA"))
                .andExpect(jsonPath("$.sesion.numeroTicket").value("PF-2026-000010"));

        var cancelled = new ReservaResponse(id, "PF-RES-2026-000001", spaceId, "C-01", "P1",
                "ABC123", TipoEspacio.CARRO, start, end, EstadoReserva.CANCELADA, null, "VIP", "Cliente canceló", start);
        when(service.cancelar(eq(id), eq(userId), any())).thenReturn(cancelled);

        mockMvc.perform(patch("/api/v1/reservas/{id}/cancelar", id)
                        .contentType("application/json")
                        .content("{\"motivo\":\"Cliente canceló\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }
}
