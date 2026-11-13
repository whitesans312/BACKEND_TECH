package com.parkflow.billing.infrastructure;

import com.parkflow.billing.domain.Comprobante;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComprobanteRepository extends JpaRepository<Comprobante, UUID> {
    Optional<Comprobante> findByPago_Id(UUID pagoId);
    Optional<Comprobante> findByLiquidacion_Id(UUID liquidacionId);
}
