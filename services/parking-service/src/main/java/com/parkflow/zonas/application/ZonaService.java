package com.parkflow.zonas.application;

import com.parkflow.pisos.domain.Piso;
import com.parkflow.pisos.infrastructure.PisoRepository;
import com.parkflow.shared.error.ConflictException;
import com.parkflow.shared.error.ResourceNotFoundException;
import com.parkflow.zonas.api.*;
import com.parkflow.zonas.domain.Zona;
import com.parkflow.zonas.infrastructure.ZonaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ZonaService {
    private final ZonaRepository zonas;
    private final PisoRepository pisos;
    public ZonaService(ZonaRepository zonas, PisoRepository pisos) { this.zonas=zonas; this.pisos=pisos; }
    @Transactional public ZonaResponse crear(ZonaRequest r) {
        Piso piso=pisos.findById(r.pisoId()).orElseThrow(()->new ResourceNotFoundException("No existe el piso indicado."));
        if (zonas.existsByPiso_IdAndCodigo(r.pisoId(), r.codigo().trim())) throw new ConflictException("Ya existe una zona con ese código en el piso.");
        return ZonaResponse.from(zonas.saveAndFlush(new Zona(piso,r.codigo().trim(),r.nombre().trim(),r.descripcion())));
    }
    @Transactional(readOnly=true) public List<ZonaResponse> listar(UUID pisoId) {
        List<Zona> result=pisoId==null?zonas.findAll():zonas.findByPiso_IdOrderByCodigo(pisoId);
        return result.stream().map(ZonaResponse::from).toList();
    }
    @Transactional public ZonaResponse actualizar(UUID id, ZonaRequest r) {
        Zona zona=zonas.findById(id).orElseThrow(()->new ResourceNotFoundException("No existe la zona solicitada."));
        Piso piso=pisos.findById(r.pisoId()).orElseThrow(()->new ResourceNotFoundException("No existe el piso indicado."));
        String codigo=r.codigo().trim();
        if(zonas.existsByPiso_IdAndCodigoAndIdNot(r.pisoId(), codigo, id)) throw new ConflictException("Ya existe una zona con ese código en el piso.");
        if(!zona.getPiso().getId().equals(r.pisoId()) && zonasRepositoryHasActiveSpaces(id)) throw new ConflictException("No se puede mover una zona con vehículos activos.");
        zona.updateDetails(piso,codigo,r.nombre().trim());
        return ZonaResponse.from(zonas.saveAndFlush(zona));
    }
    @Transactional public ZonaResponse habilitar(UUID id, boolean activa) {
        Zona zona=zonas.findById(id).orElseThrow(()->new ResourceNotFoundException("No existe la zona solicitada."));
        if(!activa && zonasRepositoryHasActiveSpaces(id)) throw new ConflictException("No se puede deshabilitar la zona: tiene vehículos activos.");
        zona.setActiva(activa);
        return ZonaResponse.from(zonas.saveAndFlush(zona));
    }
    private boolean zonasRepositoryHasActiveSpaces(UUID id) { return zonas.existsActiveSession(id); }
}
