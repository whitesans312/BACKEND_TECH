package com.parkflow.espacios.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.parkflow.espacios.api.EspacioRequest;
import com.parkflow.espacios.domain.Espacio;
import com.parkflow.espacios.domain.TipoEspacio;
import com.parkflow.espacios.infrastructure.EspacioRepository;
import com.parkflow.shared.error.ConflictException;
import com.parkflow.shared.error.ResourceNotFoundException;
import com.parkflow.zonas.domain.Zona;
import com.parkflow.zonas.infrastructure.ZonaRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EspacioServiceTest {
    @Mock EspacioRepository espacios;
    @Mock ZonaRepository zonas;
    EspacioService service;
    @BeforeEach void setup(){service=new EspacioService(espacios,zonas);}

    @Test void requiresAnExistingZone() {
        UUID zoneId=UUID.randomUUID();
        when(zonas.findById(zoneId)).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.create(new EspacioRequest(zoneId,"A01",TipoEspacio.CARRO)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(espacios,never()).saveAndFlush(any());
    }

    @Test void rejectsDuplicateSpaceCode() {
        UUID zoneId=UUID.randomUUID();
        when(zonas.findById(zoneId)).thenReturn(Optional.of(mock(Zona.class)));
        when(espacios.existsByCodigo("A01")).thenReturn(true);
        assertThatThrownBy(()->service.create(new EspacioRequest(zoneId,"A01",TipoEspacio.CARRO)))
                .isInstanceOf(ConflictException.class).hasMessageContaining("código");
        verify(espacios,never()).saveAndFlush(any());
    }

    @Test void cannotDisableSpaceWithActiveSession() {
        UUID id=UUID.randomUUID();
        when(espacios.findByIdForUpdate(id)).thenReturn(Optional.of(mock(Espacio.class)));
        when(espacios.existsActiveSession(id)).thenReturn(true);
        assertThatThrownBy(()->service.setEnabled(id,false)).isInstanceOf(ConflictException.class);
        verify(espacios,never()).saveAndFlush(any());
    }
}
