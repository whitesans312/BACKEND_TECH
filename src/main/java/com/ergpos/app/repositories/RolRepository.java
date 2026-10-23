package com.ergpos.app.repositories;

import java.util.UUID;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ergpos.app.model.Rol;

public interface RolRepository extends JpaRepository<Rol, UUID> {
    Rol findByNombre(String nombre);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Rol r WHERE r.nombre = 'ADMIN'")
    Optional<Rol> findAdminForBootstrap();
}
