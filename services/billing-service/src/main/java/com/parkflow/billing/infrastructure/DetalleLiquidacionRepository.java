package com.parkflow.billing.infrastructure;

import com.parkflow.billing.domain.DetalleLiquidacion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleLiquidacionRepository extends JpaRepository<DetalleLiquidacion, UUID> { }
