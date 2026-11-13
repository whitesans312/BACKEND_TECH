package com.parkflow.pisos.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parkflow.espacios.infrastructure.EspacioRepository;
import com.parkflow.pisos.api.PisoRequest;
import com.parkflow.pisos.api.PisoResponse;
import com.parkflow.pisos.domain.Piso;
import com.parkflow.pisos.infrastructure.PisoRepository;
import com.parkflow.shared.error.ConflictException;
import com.parkflow.shared.error.ResourceNotFoundException;

@Service
public class PisoService {

    private final PisoRepository pisoRepository;
    private final EspacioRepository espacioRepository;

    public PisoService(PisoRepository pisoRepository, EspacioRepository espacioRepository) {
        this.pisoRepository = pisoRepository;
        this.espacioRepository = espacioRepository;
    }

    @Transactional
    public PisoResponse create(PisoRequest request) {
        ensureCodeAvailable(request.codigo(), null);
        ensureOrderAvailable(request.orden(), null);
        Piso piso = new Piso(request.codigo(), request.nombre(), request.orden().shortValue());
        return PisoResponse.from(pisoRepository.saveAndFlush(piso));
    }

    @Transactional(readOnly = true)
    public List<PisoResponse> findAll() {
        return pisoRepository.findAll().stream().map(PisoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PisoResponse findById(UUID id) {
        return PisoResponse.from(requirePiso(id));
    }

    @Transactional
    public PisoResponse update(UUID id, PisoRequest request) {
        Piso piso = pisoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el piso solicitado."));
        ensureCodeAvailable(request.codigo(), id);
        ensureOrderAvailable(request.orden(), id);
        if (request.orden().shortValue() != piso.getOrden()
                && espacioRepository.existsActiveSessionsOnFloor(id)) {
            throw new ConflictException("No se puede cambiar el orden del piso mientras tenga sesiones activas.");
        }
        piso.updateDetails(request.codigo(), request.nombre(), request.orden().shortValue());
        return PisoResponse.from(pisoRepository.saveAndFlush(piso));
    }

    @Transactional
    public PisoResponse setEnabled(UUID id, boolean enabled) {
        Piso piso = pisoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el piso solicitado."));
        if (!enabled && espacioRepository.existsActiveSessionsOnFloor(id)) {
            throw new ConflictException("No se puede deshabilitar el piso: tiene sesiones activas.");
        }
        piso.setHabilitado(enabled);
        return PisoResponse.from(pisoRepository.saveAndFlush(piso));
    }

    private Piso requirePiso(UUID id) {
        return pisoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el piso solicitado."));
    }

    private void ensureCodeAvailable(String codigo, UUID currentId) {
        boolean exists = currentId == null
                ? pisoRepository.existsByCodigo(codigo)
                : pisoRepository.existsByCodigoAndIdNot(codigo, currentId);
        if (exists) {
            throw new ConflictException("Ya existe un piso con ese código.");
        }
    }

    private void ensureOrderAvailable(Integer order, UUID currentId) {
        short value = order.shortValue();
        boolean exists = currentId == null
                ? pisoRepository.existsByOrden(value)
                : pisoRepository.existsByOrdenAndIdNot(value, currentId);
        if (exists) {
            throw new ConflictException("Ya existe un piso con ese orden.");
        }
    }
}
