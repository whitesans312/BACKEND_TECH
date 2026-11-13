package com.parkflow.identity.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.parkflow.identity.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
    @Override @EntityGraph(attributePaths="roles") List<Usuario> findAll();
    @Override @EntityGraph(attributePaths="roles") Optional<Usuario> findById(UUID id);
    @EntityGraph(attributePaths="roles") Optional<Usuario> findWithRolesByEmailIgnoreCase(String email);
}
