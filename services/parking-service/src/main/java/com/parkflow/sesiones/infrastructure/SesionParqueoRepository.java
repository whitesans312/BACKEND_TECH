package com.parkflow.sesiones.infrastructure;

import com.parkflow.sesiones.domain.EstadoSesion;
import com.parkflow.sesiones.domain.SesionParqueo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface SesionParqueoRepository extends JpaRepository<SesionParqueo, UUID> {
    Optional<SesionParqueo> findByNumeroTicket(String numeroTicket);
    Optional<SesionParqueo> findByClaveIdempotencia(UUID key);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from SesionParqueo s where s.numeroTicket = :ticket")
    Optional<SesionParqueo> findByNumeroTicketForUpdate(@org.springframework.data.repository.query.Param("ticket") String ticket);
    List<SesionParqueo> findAllByOrderByFechaEntradaDesc();
    List<SesionParqueo> findByEstadoOrderByFechaEntradaDesc(EstadoSesion estado);
    List<SesionParqueo> findByPlacaActualOrderByFechaEntradaDesc(String placaActual);
    boolean existsByPlacaActualAndEstado(String placaActual, EstadoSesion estado);
}
