package com.parkflow.billing.infrastructure;

import com.parkflow.billing.domain.Caja;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CajaRepository extends JpaRepository<Caja,UUID> {
    boolean existsByCodigoIgnoreCase(String codigo);
    Optional<Caja> findByIdAndActivaTrue(UUID id);
    List<Caja> findAllByOrderByCodigoAsc();
}
