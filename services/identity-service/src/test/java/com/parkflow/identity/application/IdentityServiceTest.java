package com.parkflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import com.parkflow.identity.api.*;
import com.parkflow.identity.domain.*;
import com.parkflow.identity.infrastructure.*;
import com.parkflow.identity.security.TokenIssuer;
import com.parkflow.identity.shared.ConflictException;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {
    @Mock UsuarioRepository usuarios;
    @Mock RolRepository roles;
    @Mock TokenIssuer tokens;
    @Mock JdbcTemplate jdbc;
    private IdentityService service;
    private BCryptPasswordEncoder encoder;
    @BeforeEach void setup(){encoder=new BCryptPasswordEncoder(4);service=new IdentityService(usuarios,roles,encoder,tokens,jdbc);lenient().when(jdbc.queryForObject("select id from identity_bootstrap_lock where id=1 for update",Short.class)).thenReturn((short)1);}

    @Test void firstRegistrationBootstrapsAdministratorWithHashedPassword(){
        Rol operator=role("ADMINISTRADOR");
        when(usuarios.count()).thenReturn(0L);
        when(usuarios.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(roles.findByNombre("ADMINISTRADOR")).thenReturn(Optional.of(operator));
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(call->call.getArgument(0));
        var result=service.register(new RegistroRequest("Persona"," PERSON@Example.com ","CorrectHorseBattery12"));
        assertThat(result.email()).isEqualTo("person@example.com");
        assertThat(result.roles()).containsExactly("ADMINISTRADOR");
        var saved=org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("CorrectHorseBattery12");
        assertThat(encoder.matches("CorrectHorseBattery12",saved.getValue().getPasswordHash())).isTrue();
    }

    @Test void publicBootstrapIsClosedAfterFirstUserExists(){
        when(usuarios.count()).thenReturn(1L);
        assertThatThrownBy(()->service.register(new RegistroRequest("Persona","person@example.com","CorrectHorseBattery12")))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(roles);
        verify(usuarios,never()).saveAndFlush(any());
    }

    @Test void rejectsDuplicateEmail(){
        when(usuarios.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);
        assertThatThrownBy(()->service.register(new RegistroRequest("Persona","PERSON@example.com","CorrectHorseBattery12"))).isInstanceOf(ConflictException.class);
        verify(usuarios,never()).saveAndFlush(any());
    }

    @Test void rejectsLoginForUnknownUser(){
        when(usuarios.findWithRolesByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.login(new LoginRequest("missing@example.com","CorrectHorseBattery12"))).isInstanceOf(IdentityService.InvalidCredentialsException.class);
    }

    @Test void rejectsIncorrectPassword(){
        Usuario user=new Usuario("Persona","person@example.com",encoder.encode("CorrectHorseBattery12"));
        when(usuarios.findWithRolesByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        assertThatThrownBy(()->service.login(new LoginRequest("person@example.com","IncorrectHorseBattery12"))).isInstanceOf(IdentityService.InvalidCredentialsException.class);
        verifyNoInteractions(tokens);
    }

    @Test void rejectsDisabledUserEvenWithCorrectPassword(){
        Usuario user=new Usuario("Persona","person@example.com",encoder.encode("CorrectHorseBattery12"));user.setEstado(EstadoUsuario.INACTIVO);
        when(usuarios.findWithRolesByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        assertThatThrownBy(()->service.login(new LoginRequest("person@example.com","CorrectHorseBattery12"))).isInstanceOf(IdentityService.InvalidCredentialsException.class);
    }

    @Test void disabledUserCannotReadCurrentIdentity(){
        UUID id=UUID.randomUUID();Usuario user=new Usuario("Persona","person@example.com","hash");user.setEstado(EstadoUsuario.INACTIVO);
        when(usuarios.findById(id)).thenReturn(Optional.of(user));
        assertThatThrownBy(()->service.currentUser(id)).isInstanceOf(IdentityService.InvalidCredentialsException.class);
    }

    @Test void successfulLoginReturnsBearerToken(){
        Usuario user=new Usuario("Persona","person@example.com",encoder.encode("CorrectHorseBattery12"));user.addRol(role("OPERADOR"));
        when(usuarios.findWithRolesByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        var expires=java.time.Instant.now().plusSeconds(900);
        when(tokens.issue(user)).thenReturn(new IdentityService.IssuedToken("signed.jwt.value",expires));
        var result=service.login(new LoginRequest("PERSON@example.com","CorrectHorseBattery12"));
        assertThat(result.accessToken()).isEqualTo("signed.jwt.value");assertThat(result.tokenType()).isEqualTo("Bearer");assertThat(result.usuario().roles()).contains("OPERADOR");
    }

    private Rol role(String name){Rol role=mock(Rol.class);when(role.getNombre()).thenReturn(name);return role;}
}
