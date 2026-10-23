package com.ergpos.app.services;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ergpos.app.model.Rol;
import com.ergpos.app.model.Usuario;
import com.ergpos.app.repositories.RolRepository;
import com.ergpos.app.repositories.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Usuario> login(String email, String password) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);
        if (usuario.isEmpty() || !Boolean.TRUE.equals(usuario.get().getActivo())) {
            return Optional.empty();
        }

        Usuario encontrado = usuario.get();
        String passwordGuardada = encontrado.getPassword();

        if (isBcryptHash(passwordGuardada) && passwordEncoder.matches(password, passwordGuardada)) {
            return Optional.of(encontrado);
        }

        if (!isBcryptHash(passwordGuardada) && passwordGuardada.equals(password)) {
            encontrado.setPassword(passwordEncoder.encode(password));
            return Optional.of(usuarioRepository.save(encontrado));
        }

        return Optional.empty();
    }

    public Usuario register(Usuario usuario) {
        normalizar(usuario);

        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new EmailAlreadyExistsException("El email ya está registrado: " + usuario.getEmail());
        }
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return usuarioRepository.save(usuario);
    }

    /** Crea el único administrador inicial si todavía no existe ningún usuario. */
    @Transactional
    public Usuario bootstrapAdmin(Usuario usuario) {
        Rol admin = rolRepository.findAdminForBootstrap()
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el rol ADMIN. Ejecute primero el instalador SQL."));

        // El bloqueo pesimista del rol ADMIN serializa intentos simultáneos de bootstrap.
        if (usuarioRepository.count() > 0) {
            throw new IllegalStateException("El administrador inicial ya fue creado.");
        }

        normalizar(usuario);
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new EmailAlreadyExistsException("El email ya está registrado: " + usuario.getEmail());
        }

        usuario.setId(null);
        usuario.setRol(admin);
        usuario.setActivo(true);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return usuarioRepository.save(usuario);
    }

    private boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    private void normalizar(Usuario usuario) {
        if (usuario.getNombre() != null) {
            usuario.setNombre(usuario.getNombre().trim());
        }
        if (usuario.getEmail() != null) {
            usuario.setEmail(usuario.getEmail().trim().toLowerCase());
        }
        if (usuario.getTelefono() != null) {
            String telefono = usuario.getTelefono().trim();
            usuario.setTelefono(telefono.isBlank() ? null : telefono);
        }
    }

    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String message) {
            super(message);
        }
    }
}
