package com.parkflow.configuracion.application;

import com.parkflow.configuracion.api.ParqueaderoSetupRequest;
import com.parkflow.configuracion.api.ParqueaderoSetupResponse;
import com.parkflow.espacios.domain.Espacio;
import com.parkflow.espacios.domain.TipoEspacio;
import com.parkflow.espacios.infrastructure.EspacioRepository;
import com.parkflow.pisos.api.PisoResponse;
import com.parkflow.pisos.domain.Piso;
import com.parkflow.pisos.infrastructure.PisoRepository;
import com.parkflow.shared.error.ConflictException;
import com.parkflow.zonas.api.ZonaResponse;
import com.parkflow.zonas.domain.Zona;
import com.parkflow.zonas.infrastructure.ZonaRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParqueaderoSetupService {
    private final PisoRepository pisos;
    private final ZonaRepository zonas;
    private final EspacioRepository espacios;

    public ParqueaderoSetupService(PisoRepository pisos, ZonaRepository zonas, EspacioRepository espacios) {
        this.pisos = pisos; this.zonas = zonas; this.espacios = espacios;
    }

    @Transactional
    public ParqueaderoSetupResponse create(ParqueaderoSetupRequest request) {
        String pisoCodigo = request.codigoPiso().trim().toUpperCase(Locale.ROOT);
        if (pisos.existsByCodigo(pisoCodigo)) throw new ConflictException("Ya existe un piso con ese código.");
        if (pisos.existsByOrden(request.nivel().shortValue())) throw new ConflictException("Ya existe un piso con ese nivel.");
        Set<String> zoneCodes = new HashSet<>();
        Set<String> spaceCodes = new HashSet<>();
        for (var zone : request.zonas()) {
            String zoneCode = zone.codigo().trim().toUpperCase(Locale.ROOT);
            if (!zoneCodes.add(zoneCode))
                throw new ConflictException("Hay códigos de zona repetidos.");
            collectCodes(spaceCodes, pisoCodigo, zoneCode, TipoEspacio.CARRO, zone.espaciosCarro());
            collectCodes(spaceCodes, pisoCodigo, zoneCode, TipoEspacio.MOTO, zone.espaciosMoto());
        }
        if (spaceCodes.stream().anyMatch(espacios::existsByCodigo))
            throw new ConflictException("Un código de espacio generado ya existe. Cambia el código de zona para evitar duplicados.");

        Piso piso = pisos.saveAndFlush(new Piso(pisoCodigo, request.nombrePiso().trim(), request.nivel().shortValue()));
        List<ZonaResponse> createdZones = new ArrayList<>();
        int count = 0;
        for (var spec : request.zonas()) {
            String zoneCode = spec.codigo().trim().toUpperCase(Locale.ROOT);
            Zona zone = zonas.saveAndFlush(new Zona(piso, zoneCode, spec.nombre().trim(), null));
            createdZones.add(ZonaResponse.from(zone));
            count += createSpaces(zone, pisoCodigo, zoneCode, TipoEspacio.CARRO, spec.espaciosCarro());
            count += createSpaces(zone, pisoCodigo, zoneCode, TipoEspacio.MOTO, spec.espaciosMoto());
        }
        return new ParqueaderoSetupResponse(PisoResponse.from(piso), createdZones, count);
    }

    private void collectCodes(Set<String> codes, String floorCode, String zoneCode, TipoEspacio type, int count) {
        if (count > 999) throw new ConflictException("Cada zona admite máximo 999 espacios por tipo.");
        String suffix = type == TipoEspacio.CARRO ? "C" : "M";
        for (int i = 1; i <= count; i++) {
            String code = floorCode + "-" + zoneCode + "-" + suffix + String.format(Locale.ROOT, "%02d", i);
            if (code.length() > 20) throw new ConflictException("La combinación de códigos de piso y zona es demasiado larga para generar los espacios (máximo 20 caracteres).");
            if (!codes.add(code)) throw new ConflictException("Se generarían códigos de espacios repetidos.");
        }
    }

    private int createSpaces(Zona zone, String floorCode, String zoneCode, TipoEspacio type, int count) {
        String suffix = type == TipoEspacio.CARRO ? "C" : "M";
        for (int i = 1; i <= count; i++) {
            String code = floorCode + "-" + zoneCode + "-" + suffix + String.format(Locale.ROOT, "%02d", i);
            espacios.save(new Espacio(zone, code, type));
        }
        return count;
    }
}
