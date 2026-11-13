package com.parkflow.billing.infrastructure;

import com.parkflow.billing.domain.Tarifa;
import com.parkflow.billing.domain.TipoVehiculo;
import com.parkflow.billing.domain.ConceptoTarifa;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TarifaRepository extends JpaRepository<Tarifa, UUID> {
    default Optional<Tarifa> findByTipoVehiculoAndActivaTrue(TipoVehiculo tipo) {
        return findByTipoVehiculoAndConceptoAndActivaTrue(tipo, ConceptoTarifa.HORA);
    }
    Optional<Tarifa> findByTipoVehiculoAndConceptoAndActivaTrue(TipoVehiculo tipoVehiculo, ConceptoTarifa concepto);
    List<Tarifa> findByTipoVehiculoOrderByVigenteDesdeDesc(TipoVehiculo tipoVehiculo);
    List<Tarifa> findAllByOrderByTipoVehiculoAscConceptoAscVigenteDesdeDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tarifa t where t.tipoVehiculo = :tipo and t.concepto = :concepto and t.activa = true")
    Optional<Tarifa> lockActive(@Param("tipo") TipoVehiculo tipoVehiculo, @Param("concepto") ConceptoTarifa concepto);
}
