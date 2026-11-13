package com.parkflow.billing.infrastructure;

import com.parkflow.billing.domain.EstadoLiquidacion;
import com.parkflow.billing.domain.Liquidacion;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiquidacionRepository extends JpaRepository<Liquidacion, UUID> {
    Optional<Liquidacion> findByClaveIdempotencia(UUID key);
    Optional<Liquidacion> findFirstBySesionParqueoIdAndEstadoOrderByCreadoEnDesc(UUID sessionId, EstadoLiquidacion state);
    List<Liquidacion> findBySesionParqueoIdOrderByCreadoEnDesc(UUID sessionId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Liquidacion l where l.id = :id")
    Optional<Liquidacion> lockById(@Param("id") UUID id);
}
