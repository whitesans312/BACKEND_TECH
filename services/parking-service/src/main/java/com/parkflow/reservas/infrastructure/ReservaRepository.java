package com.parkflow.reservas.infrastructure;

import com.parkflow.reservas.domain.EstadoReserva;
import com.parkflow.reservas.domain.Reserva;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    @Query("""
            select r from Reserva r
            join fetch r.espacio e
            join fetch e.zona z
            join fetch z.piso p
            where r.claveIdempotencia = :key
            """)
    Optional<Reserva> findByClaveIdempotencia(@Param("key") UUID key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select r from Reserva r
            join fetch r.espacio e
            join fetch e.zona z
            join fetch z.piso p
            where r.id = :id
            """)
    Optional<Reserva> findDetailedById(@Param("id") UUID id);

    @Query("""
            select r from Reserva r
            join fetch r.espacio e
            join fetch e.zona z
            join fetch z.piso p
            where (:estado is null or r.estado = :estado)
              and (:placa is null or r.placa = :placa)
            order by r.fechaInicio desc, r.creadoEn desc
            """)
    List<Reserva> findAllFiltered(@Param("estado") EstadoReserva estado, @Param("placa") String placa);

    boolean existsByPlacaAndEstado(String placa, EstadoReserva estado);

    @Query("""
            select r from Reserva r
            where r.estado = com.parkflow.reservas.domain.EstadoReserva.ACTIVA
              and r.fechaFin <= :ahora
              and (r.espacio.id = :espacioId or r.placa = :placa)
            """)
    List<Reserva> findExpiredActiveForSpaceOrPlate(@Param("espacioId") UUID espacioId,
                                                   @Param("placa") String placa,
                                                   @Param("ahora") Instant ahora);

    @Query("""
            select count(r) > 0 from Reserva r
            where r.espacio.id = :espacioId
              and r.estado = com.parkflow.reservas.domain.EstadoReserva.ACTIVA
              and r.fechaInicio < :fechaFin
              and r.fechaFin > :fechaInicio
              and r.fechaFin > :ahora
            """)
    boolean existsOverlappingActiveReservation(@Param("espacioId") UUID espacioId,
                                               @Param("fechaInicio") Instant fechaInicio,
                                               @Param("fechaFin") Instant fechaFin,
                                               @Param("ahora") Instant ahora);

    @Modifying
    @Query("""
            update Reserva r
               set r.estado = com.parkflow.reservas.domain.EstadoReserva.EXPIRADA,
                   r.actualizadoEn = :ahora
             where r.estado = com.parkflow.reservas.domain.EstadoReserva.ACTIVA
               and r.fechaFin <= :ahora
            """)
    int expirePastDue(@Param("ahora") Instant ahora);
}
