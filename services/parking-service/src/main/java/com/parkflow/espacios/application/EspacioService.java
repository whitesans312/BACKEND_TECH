package com.parkflow.espacios.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parkflow.espacios.api.EspacioRequest;
import com.parkflow.espacios.api.EspacioResponse;
import com.parkflow.espacios.domain.Espacio;
import com.parkflow.espacios.infrastructure.EspacioRepository;
import com.parkflow.zonas.domain.Zona;
import com.parkflow.zonas.infrastructure.ZonaRepository;
import com.parkflow.shared.error.ConflictException;
import com.parkflow.shared.error.ResourceNotFoundException;

@Service
public class EspacioService {

    private final EspacioRepository espacioRepository;
    private final ZonaRepository zonaRepository;

    public EspacioService(EspacioRepository espacioRepository, ZonaRepository zonaRepository) {
        this.espacioRepository = espacioRepository;
        this.zonaRepository = zonaRepository;
    }

    @Transactional
    public EspacioResponse create(EspacioRequest request) {
        Zona zona = requireZona(request.zonaId());
        ensureCodeAvailable(request.codigo(), null);
        Espacio espacio = new Espacio(zona, request.codigo(), request.tipo());
        Espacio saved = espacioRepository.saveAndFlush(espacio);
        return findById(saved.getId());
    }

    @Transactional(readOnly = true)
    public List<EspacioResponse> findAll(UUID pisoId) {
        if (pisoId != null) {
            zonaRepository.findByPiso_IdOrderByCodigo(pisoId);
        }
        return espacioRepository.findAvailability(pisoId).stream().map(EspacioResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EspacioResponse findById(UUID id) {
        return espacioRepository.findAvailabilityById(id)
                .map(EspacioResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el espacio solicitado."));
    }

    @Transactional
    public EspacioResponse update(UUID id, EspacioRequest request) {
        Espacio espacio = espacioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el espacio solicitado."));
        Zona zona = requireZona(request.zonaId());
        boolean assignmentDetailsChanged = !Objects.equals(espacio.getZona().getId(), request.zonaId())
                || !Objects.equals(espacio.getTipo(), request.tipo())
                || !Objects.equals(espacio.getCodigo(), request.codigo());
        if (assignmentDetailsChanged && espacioRepository.existsActiveSession(id)) {
            throw new ConflictException(
                    "No se puede cambiar el código, tipo o piso de un espacio mientras tenga una sesión activa.");
        }
        ensureCodeAvailable(request.codigo(), id);
        espacio.updateDetails(zona, request.codigo(), request.tipo());
        espacioRepository.saveAndFlush(espacio);
        return findById(id);
    }

    @Transactional
    public EspacioResponse setEnabled(UUID id, boolean enabled) {
        Espacio espacio = espacioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el espacio solicitado."));
        if (!enabled && espacioRepository.existsActiveSession(id)) {
            throw new ConflictException("No se puede deshabilitar el espacio: tiene una sesión activa.");
        }
        espacio.setHabilitado(enabled);
        espacioRepository.saveAndFlush(espacio);
        return findById(id);
    }

    private Zona requireZona(UUID id) {
        return zonaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la zona indicada para el espacio."));
    }

    private void ensureCodeAvailable(String codigo, UUID currentId) {
        boolean exists = currentId == null
                ? espacioRepository.existsByCodigo(codigo)
                : espacioRepository.existsByCodigoAndIdNot(codigo, currentId);
        if (exists) {
            throw new ConflictException("Ya existe un espacio con ese código.");
        }
    }
}
