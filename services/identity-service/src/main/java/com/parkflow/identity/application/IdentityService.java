package com.parkflow.identity.application;

import com.parkflow.identity.api.*;
import com.parkflow.identity.domain.*;
import com.parkflow.identity.infrastructure.*;
import com.parkflow.identity.security.TokenIssuer;
import com.parkflow.identity.shared.ConflictException;
import com.parkflow.identity.shared.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder passwords;
    private final TokenIssuer tokens;
    private final JdbcTemplate jdbc;
    private final String dummyPasswordHash;

    public IdentityService(UsuarioRepository usuarios,RolRepository roles,PasswordEncoder passwords,TokenIssuer tokens,JdbcTemplate jdbc){
        this.usuarios=usuarios;this.roles=roles;this.passwords=passwords;this.tokens=tokens;this.jdbc=jdbc;
        this.dummyPasswordHash=passwords.encode(UUID.randomUUID().toString());
    }

    /** One-time bootstrap. The database row lock makes competing first-user requests serialize. */
    @Transactional public UsuarioResponse register(RegistroRequest r){
        jdbc.queryForObject("select id from identity_bootstrap_lock where id=1 for update",Short.class);
        if(usuarios.count()!=0)throw new ConflictException("El administrador inicial ya fue configurado. Un administrador debe crear usuarios nuevos.");
        ensureEmailAvailable(r.email(),null);
        Rol admin=roles.findByNombre("ADMINISTRADOR").orElseThrow(()->new IllegalStateException("La migración debe crear el rol ADMINISTRADOR."));
        Usuario user=new Usuario(r.nombre().strip(),r.email(),passwords.encode(r.password()));
        user.addRol(admin);
        return UsuarioResponse.from(usuarios.saveAndFlush(user));
    }

    @Transactional public UsuarioResponse create(RegistroRequest r,Set<UUID> roleIds){
        ensureEmailAvailable(r.email(),null);
        Usuario user=new Usuario(r.nombre().strip(),r.email(),passwords.encode(r.password()));
        user.setRoles(resolveRoles(roleIds));
        return UsuarioResponse.from(usuarios.saveAndFlush(user));
    }

    @Transactional(readOnly=true) public TokenResponse login(LoginRequest request){
        Usuario user=usuarios.findWithRolesByEmailIgnoreCase(request.email()).orElse(null);
        if(user==null){passwords.matches(request.password(),dummyPasswordHash);throw new InvalidCredentialsException();}
        boolean matches=passwords.matches(request.password(),user.getPasswordHash());
        if(user.getEstado()!=EstadoUsuario.ACTIVO||!matches)throw new InvalidCredentialsException();
        IssuedToken issued=tokens.issue(user);
        return new TokenResponse(issued.value(),"Bearer",issued.expiresAt(),UsuarioResponse.from(user));
    }

    @Transactional(readOnly=true) public UsuarioResponse currentUser(UUID id){Usuario user=requireUser(id);if(user.getEstado()!=EstadoUsuario.ACTIVO)throw new InvalidCredentialsException();return UsuarioResponse.from(user);}
    @Transactional(readOnly=true) public List<UsuarioResponse> findAll(){return usuarios.findAll().stream().map(UsuarioResponse::from).toList();}
    @Transactional(readOnly=true) public UsuarioResponse findById(UUID id){return UsuarioResponse.from(requireUser(id));}
    @Transactional public UsuarioResponse update(UUID id,ActualizarUsuarioRequest request){Usuario user=requireUser(id);ensureEmailAvailable(request.email(),id);user.updateDetails(request.nombre().strip(),request.email());return UsuarioResponse.from(usuarios.saveAndFlush(user));}
    @Transactional public UsuarioResponse setStatus(UUID id,EstadoUsuario status){Usuario user=requireUser(id);user.setEstado(status);return UsuarioResponse.from(usuarios.saveAndFlush(user));}
    @Transactional public UsuarioResponse setRoles(UUID id,Set<UUID> roleIds){Usuario user=requireUser(id);user.setRoles(resolveRoles(roleIds));return UsuarioResponse.from(usuarios.saveAndFlush(user));}
    @Transactional(readOnly=true) public List<RolResponse> findRoles(){return roles.findAllByOrderByNombreAsc().stream().map(RolResponse::from).toList();}

    private Usuario requireUser(UUID id){return usuarios.findById(id).orElseThrow(()->new ResourceNotFoundException("No existe el usuario solicitado."));}
    private void ensureEmailAvailable(String email,UUID current){boolean found=current==null?usuarios.existsByEmailIgnoreCase(email):usuarios.existsByEmailIgnoreCaseAndIdNot(email,current);if(found)throw new ConflictException("Ya existe un usuario con ese correo.");}
    private Set<Rol> resolveRoles(Set<UUID> ids){Set<Rol> found=roles.findByIdIn(ids);if(found.size()!=ids.size())throw new ResourceNotFoundException("Uno o más roles no existen.");return found;}

    public record IssuedToken(String value,Instant expiresAt){}
    public static class InvalidCredentialsException extends RuntimeException{public InvalidCredentialsException(){super("Credenciales inválidas.");}}
}
