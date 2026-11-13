package com.parkflow.pisos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkflow.espacios.infrastructure.EspacioRepository;
import com.parkflow.pisos.api.PisoRequest;
import com.parkflow.pisos.domain.Piso;
import com.parkflow.pisos.infrastructure.PisoRepository;
import com.parkflow.shared.error.ConflictException;
import com.parkflow.shared.error.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class PisoServiceTest {

    @Mock
    private PisoRepository pisoRepository;

    @Mock
    private EspacioRepository espacioRepository;

    private PisoService service;

    @BeforeEach
    void setUp() {
        service = new PisoService(pisoRepository, espacioRepository);
    }

    @Test
    void createsEnabledFloorWhenCodeAndOrderAreUnique() {
        when(pisoRepository.existsByCodigo("P1")).thenReturn(false);
        when(pisoRepository.existsByOrden((short) 1)).thenReturn(false);
        when(pisoRepository.saveAndFlush(any(Piso.class))).thenAnswer(call -> call.getArgument(0));

        var created = service.create(new PisoRequest("P1", "Piso uno", 1));

        assertThat(created.codigo()).isEqualTo("P1");
        assertThat(created.orden()).isEqualTo((short) 1);
        assertThat(created.habilitado()).isTrue();
    }

    @Test
    void rejectsDuplicateFloorCode() {
        when(pisoRepository.existsByCodigo("P1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new PisoRequest("P1", "Piso uno", 1)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("código");
        verify(pisoRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateFloorOrder() {
        when(pisoRepository.existsByCodigo("P2")).thenReturn(false);
        when(pisoRepository.existsByOrden((short) 1)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new PisoRequest("P2", "Piso dos", 1)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("orden");
        verify(pisoRepository, never()).saveAndFlush(any());
    }

    @Test
    void cannotDisableFloorWithAnActiveSession() {
        UUID id = UUID.randomUUID();
        when(pisoRepository.findByIdForUpdate(id)).thenReturn(Optional.of(new Piso("P1", "Piso uno", (short) 1)));
        when(espacioRepository.existsActiveSessionsOnFloor(id)).thenReturn(true);

        assertThatThrownBy(() -> service.setEnabled(id, false))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("sesiones activas");
        verify(pisoRepository, never()).saveAndFlush(any());
    }

    @Test
    void cannotChangeFloorOrderWhileItHasActiveSessions() {
        UUID id = UUID.randomUUID();
        when(pisoRepository.findByIdForUpdate(id)).thenReturn(Optional.of(new Piso("P1", "Piso uno", (short) 1)));
        when(pisoRepository.existsByCodigoAndIdNot("P1", id)).thenReturn(false);
        when(pisoRepository.existsByOrdenAndIdNot((short) 2, id)).thenReturn(false);
        when(espacioRepository.existsActiveSessionsOnFloor(id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, new PisoRequest("P1", "Piso uno", 2)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("orden");
        verify(pisoRepository, never()).saveAndFlush(any());
    }

    @Test
    void allowsFloorMetadataUpdateWhileOccupiedWhenAssignmentOrderIsUnchanged() {
        UUID id = UUID.randomUUID();
        when(pisoRepository.findByIdForUpdate(id)).thenReturn(Optional.of(new Piso("P1", "Piso uno", (short) 1)));
        when(pisoRepository.existsByCodigoAndIdNot("P1", id)).thenReturn(false);
        when(pisoRepository.existsByOrdenAndIdNot((short) 1, id)).thenReturn(false);
        when(pisoRepository.saveAndFlush(any(Piso.class))).thenAnswer(call -> call.getArgument(0));

        var updated = service.update(id, new PisoRequest("P1", "Nivel principal", 1));

        assertThat(updated.nombre()).isEqualTo("Nivel principal");
    }

    @Test
    void returnsNotFoundForUnknownFloor() {
        UUID id = UUID.randomUUID();
        when(pisoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }
}
