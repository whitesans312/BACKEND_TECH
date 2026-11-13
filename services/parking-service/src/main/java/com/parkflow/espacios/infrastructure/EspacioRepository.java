package com.parkflow.espacios.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.parkflow.espacios.domain.Espacio;

import jakarta.persistence.LockModeType;

public interface EspacioRepository extends JpaRepository<Espacio, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Espacio e where e.id = :id")
    Optional<Espacio> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);

    @Query(value = """
            SELECT e.id AS "id",
                   p.id AS "pisoId",
                   z.id AS "zonaId",
                   p.codigo AS "pisoCodigo",
                   e.codigo AS "codigo",
                   e.tipo_vehiculo AS "tipo",
                   (e.estado = 'HABILITADO') AS "habilitado",
                   (p.activo AND z.activa) AS "pisoHabilitado",
                   EXISTS (SELECT 1 FROM sesiones_parqueo s
                           WHERE s.espacio_id = e.id AND s.estado = 'ACTIVA') AS "ocupado",
                   e.creado_en AS "createdAt",
                   e.actualizado_en AS "updatedAt"
              FROM espacios e
              JOIN zonas z ON z.id = e.zona_id
              JOIN pisos p ON p.id = z.piso_id
             WHERE (:pisoId IS NULL OR p.id = :pisoId)
             ORDER BY p.numero, z.codigo, e.codigo
            """, nativeQuery = true)
    List<EspacioAvailabilityView> findAvailability(@Param("pisoId") UUID pisoId);

    @Query(value = """
            SELECT e.id AS "id",
                   p.id AS "pisoId",
                   z.id AS "zonaId",
                   p.codigo AS "pisoCodigo",
                   e.codigo AS "codigo",
                   e.tipo_vehiculo AS "tipo",
                   (e.estado = 'HABILITADO') AS "habilitado",
                   (p.activo AND z.activa) AS "pisoHabilitado",
                   EXISTS (SELECT 1 FROM sesiones_parqueo s
                           WHERE s.espacio_id = e.id AND s.estado = 'ACTIVA') AS "ocupado",
                   e.creado_en AS "createdAt",
                   e.actualizado_en AS "updatedAt"
              FROM espacios e
              JOIN zonas z ON z.id = e.zona_id
              JOIN pisos p ON p.id = z.piso_id
             WHERE e.id = :id
            """, nativeQuery = true)
    Optional<EspacioAvailabilityView> findAvailabilityById(@Param("id") UUID id);

    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM sesiones_parqueo s
                           WHERE s.espacio_id = :espacioId AND s.estado = 'ACTIVA')
            """, nativeQuery = true)
    boolean existsActiveSession(@Param("espacioId") UUID espacioId);

    @Query(value = """
            SELECT EXISTS (SELECT 1
                             FROM sesiones_parqueo s
                             JOIN espacios e ON e.id = s.espacio_id
                             JOIN zonas z ON z.id = e.zona_id
                            WHERE z.piso_id = :pisoId AND s.estado = 'ACTIVA')
            """, nativeQuery = true)
    boolean existsActiveSessionsOnFloor(@Param("pisoId") UUID pisoId);
}
