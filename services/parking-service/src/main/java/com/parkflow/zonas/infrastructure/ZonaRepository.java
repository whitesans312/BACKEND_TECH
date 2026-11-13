package com.parkflow.zonas.infrastructure;

import com.parkflow.zonas.domain.Zona;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZonaRepository extends JpaRepository<Zona, UUID> {
    List<Zona> findByPiso_IdOrderByCodigo(UUID pisoId);
    boolean existsByPiso_IdAndCodigo(UUID pisoId, String codigo);
}
