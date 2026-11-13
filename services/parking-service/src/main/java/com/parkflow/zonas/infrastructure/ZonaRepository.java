package com.parkflow.zonas.infrastructure;

import com.parkflow.zonas.domain.Zona;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ZonaRepository extends JpaRepository<Zona, UUID> {
    List<Zona> findByPiso_IdOrderByCodigo(UUID pisoId);
    boolean existsByPiso_IdAndCodigo(UUID pisoId, String codigo);
    boolean existsByPiso_IdAndCodigoAndIdNot(UUID pisoId, String codigo, UUID id);
    @Query(value = "SELECT EXISTS (SELECT 1 FROM sesiones_parqueo s JOIN espacios e ON e.id=s.espacio_id WHERE e.zona_id=:zonaId AND s.estado='ACTIVA')", nativeQuery = true)
    boolean existsActiveSession(@Param("zonaId") UUID zonaId);
}
