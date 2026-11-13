package com.parkflow.identity.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.parkflow.identity.domain.Rol;

public interface RolRepository extends JpaRepository<Rol, UUID> {
    Optional<Rol> findByNombre(String nombre);
    List<Rol> findAllByOrderByNombreAsc();
    Set<Rol> findByNombreIn(Set<String> nombres);
    Set<Rol> findByIdIn(Set<UUID> ids);
}
