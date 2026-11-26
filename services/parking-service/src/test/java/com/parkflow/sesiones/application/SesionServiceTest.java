package com.parkflow.sesiones.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.parkflow.espacios.domain.Espacio;
import com.parkflow.espacios.domain.TipoEspacio;
import com.parkflow.espacios.infrastructure.EspacioRepository;
import com.parkflow.zonas.domain.Zona;
import com.parkflow.pisos.domain.Piso;
import com.parkflow.shared.error.ConflictException;
import com.parkflow.sesiones.api.EntradaRequest;
import com.parkflow.sesiones.domain.EstadoSesion;
import com.parkflow.sesiones.domain.SesionParqueo;
import com.parkflow.sesiones.infrastructure.SesionParqueoRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class SesionServiceTest {
    @Mock SesionParqueoRepository sesiones;
    @Mock EspacioRepository espacios;
    @Mock JdbcTemplate jdbc;
    SesionService service;

    @BeforeEach void setup() { service=new SesionService(sesiones,espacios,jdbc,"http://127.0.0.1:18082"); }

    @Test void rejectsPlateWithAnActiveSession() {
        when(sesiones.existsByPlacaActualAndEstado("ABC123", EstadoSesion.ACTIVA)).thenReturn(true);
        assertThatThrownBy(()->service.registrarEntrada(new EntradaRequest("abc 123",TipoEspacio.CARRO,UUID.randomUUID()),UUID.randomUUID(),UUID.randomUUID()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("sesión activa");
        verify(espacios,never()).findByIdForUpdate(any());
    }

    @Test void requiresTheManuallySelectedSpaceToMatchVehicleType() {
        UUID spaceId=UUID.randomUUID();
        Espacio space=mock(Espacio.class);
        when(sesiones.existsByPlacaActualAndEstado("ABC123",EstadoSesion.ACTIVA)).thenReturn(false);
        when(espacios.findByIdForUpdate(spaceId)).thenReturn(Optional.of(space));
        when(space.getTipo()).thenReturn(TipoEspacio.MOTO);
        var request=new EntradaRequest("abc123",TipoEspacio.CARRO,spaceId);
        assertThatThrownBy(()->service.registrarEntrada(request,UUID.randomUUID(),UUID.randomUUID())).isInstanceOf(ConflictException.class).hasMessageContaining("compatible");
        verify(jdbc,never()).queryForObject(anyString(),eq(Long.class));
    }

    @Test void usesTheOperatorSelectedAvailableSpaceAndNormalizesPlate() {
        UUID spaceId=UUID.randomUUID();
        Espacio space=mock(Espacio.class);
        Zona zone=mock(Zona.class);
        Piso floor=mock(Piso.class);
        when(sesiones.existsByPlacaActualAndEstado("ABC123",EstadoSesion.ACTIVA)).thenReturn(false);
        when(espacios.findByIdForUpdate(spaceId)).thenReturn(Optional.of(space));
        when(space.getId()).thenReturn(spaceId);
        when(space.getTipo()).thenReturn(TipoEspacio.CARRO);
        when(space.isHabilitado()).thenReturn(true);
        when(space.getZona()).thenReturn(zone);
        when(zone.isActiva()).thenReturn(true);
        when(zone.getPiso()).thenReturn(floor);
        when(floor.isHabilitado()).thenReturn(true);
        when(espacios.existsActiveSession(spaceId)).thenReturn(false);
        when(jdbc.queryForObject("select nextval('seq_ticket')",Long.class)).thenReturn(42L);
        when(sesiones.saveAndFlush(any(SesionParqueo.class))).thenAnswer(call->call.getArgument(0));

        var response=service.registrarEntrada(new EntradaRequest("abc 123",TipoEspacio.CARRO,spaceId),UUID.randomUUID(),UUID.randomUUID());

        assertThat(response.placa()).isEqualTo("ABC123");
        assertThat(response.numeroTicket()).matches("PF-[0-9]{4}-000042");
        verify(espacios).findByIdForUpdate(spaceId);
    }

    @Test void solicitarSalidaDoesNotLockRowBeforeBillingHttpCallWhenPending() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://billing-service:8083");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SesionService salidaService = new SesionService(sesiones, espacios, jdbc, builder, null);

        UUID spaceId = UUID.randomUUID();
        UUID opId = UUID.randomUUID();
        UUID settlementId = UUID.randomUUID();
        Espacio space = mock(Espacio.class);
        when(space.getId()).thenReturn(spaceId);
        when(space.getCodigo()).thenReturn("C-01");
        SesionParqueo session = new SesionParqueo("PF-2026-000099", "XYZ987", TipoEspacio.CARRO, space, UUID.randomUUID());
        when(sesiones.findByNumeroTicket("PF-2026-000099")).thenReturn(Optional.of(session));

        server.expect(requestTo("http://billing-service:8083/internal/v1/liquidaciones/by-idempotency-key/" + opId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"" + settlementId + "\",\"estado\":\"PENDIENTE\",\"total\":6000.00}", MediaType.APPLICATION_JSON));

        Jwt jwt = Jwt.withTokenValue("jwt-token").header("alg", "RS256").subject(UUID.randomUUID().toString()).build();
        var result = salidaService.solicitarSalida("PF-2026-000099", opId, jwt);

        assertThat(result.salidaConfirmada()).isFalse();
        assertThat(result.estadoLiquidacion()).isEqualTo("PENDIENTE");
        verify(sesiones).findByNumeroTicket("PF-2026-000099");
        verify(sesiones, never()).findByNumeroTicketForUpdate(any());
        server.verify();
    }
}
