package com.parkflow.billing.infrastructure;

import com.parkflow.billing.domain.SesionCaja;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionCajaRepository extends JpaRepository<SesionCaja,UUID> {
    Optional<SesionCaja> findByClaveIdempotencia(UUID key);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SesionCaja s where s.id=:id")
    Optional<SesionCaja> lockById(@Param("id") UUID id);
}
