package com.parkflow.pisos.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.parkflow.pisos.domain.Piso;

public interface PisoRepository extends JpaRepository<Piso, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Piso p where p.id = :id")
    Optional<Piso> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);

    boolean existsByOrden(Short orden);

    boolean existsByOrdenAndIdNot(Short orden, UUID id);
}
