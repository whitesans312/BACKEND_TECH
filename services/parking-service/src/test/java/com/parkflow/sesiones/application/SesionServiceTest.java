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
import org.springframework.jdbc.core.JdbcTemplate;

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
}
