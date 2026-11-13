package com.parkflow.billing.infrastructure;

import com.parkflow.billing.domain.EstadoPago;
import com.parkflow.billing.domain.Pago;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, UUID> {
    Optional<Pago> findByClaveIdempotencia(UUID key);
    Optional<Pago> findFirstByLiquidacion_IdAndEstado(UUID liquidacionId, EstadoPago estado);
    List<Pago> findByLiquidacion_SesionParqueoIdOrderByCreadoEnDesc(UUID sesionId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p join fetch p.liquidacion where p.id = :id")
    Optional<Pago> lockById(@Param("id") UUID id);
    @Query("select p from Pago p join fetch p.liquidacion where p.id = :id")
    Optional<Pago> findDetailedById(@Param("id") UUID id);
    @Query("select p from Pago p join fetch p.liquidacion where p.claveIdempotencia = :key")
    Optional<Pago> findDetailedByIdempotencyKey(@Param("key") UUID key);
    @Query("select p from Pago p join fetch p.liquidacion where p.liquidacion.sesionParqueoId = :sessionId order by p.creadoEn desc")
    List<Pago> findDetailedBySession(@Param("sessionId") UUID sessionId);
}
