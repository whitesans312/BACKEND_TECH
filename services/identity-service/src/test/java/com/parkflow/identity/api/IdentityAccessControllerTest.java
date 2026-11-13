package com.parkflow.identity.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.parkflow.identity.application.IdentityService;
import com.parkflow.identity.domain.EstadoUsuario;
import com.parkflow.identity.domain.Usuario;
import com.parkflow.identity.security.SecurityConfiguration;
import com.parkflow.identity.security.IdentityJwtAuthenticationConverter;
import com.parkflow.identity.infrastructure.UsuarioRepository;

@WebMvcTest(controllers={AuthController.class,UsuarioController.class})
@Import({SecurityConfiguration.class,IdentityJwtAuthenticationConverter.class})
class IdentityAccessControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean IdentityService service;
    @MockitoBean UsuarioRepository usuarios;

    @Test void publicRegistrationDoesNotRequireAuthentication() throws Exception {
        UUID id=UUID.randomUUID();
        when(service.register(org.mockito.ArgumentMatchers.any())).thenReturn(user(id,Set.of("ADMINISTRADOR")));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Operador\",\"email\":\" op@example.com \",\"password\":\"CorrectHorseBattery12\"}"))
            .andExpect(status().isCreated()).andExpect(header().string("Location","/api/users/"+id)).andExpect(jsonPath("$.roles[0]").value("ADMINISTRADOR"));
    }

    @Test void usersEndpointRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test void operatorCannotAccessAdministratorEndpoint() throws Exception {
        mvc.perform(get("/api/users").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_OPERADOR"))))
            .andExpect(status().isForbidden());
    }

    @Test void inactiveUserTokenIsRejectedOnProtectedIdentityRoutes() throws Exception {
        UUID id=UUID.randomUUID();Usuario inactive=org.mockito.Mockito.mock(Usuario.class);
        when(inactive.getEstado()).thenReturn(EstadoUsuario.INACTIVO);
        when(usuarios.findById(id)).thenReturn(java.util.Optional.of(inactive));
        var jwt=org.springframework.security.oauth2.jwt.Jwt.withTokenValue("inactive-token").header("alg","RS256").subject(id.toString()).build();
        when(jwtDecoder.decode("inactive-token")).thenReturn(jwt);
        mvc.perform(get("/api/users").header("Authorization","Bearer inactive-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test void administratorCanListUsers() throws Exception {
        when(service.findAll()).thenReturn(List.of(user(UUID.randomUUID(),Set.of("ADMINISTRADOR"))));
        mvc.perform(get("/api/users").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].roles[0]").value("ADMINISTRADOR"));
    }

    @Test void currentUserUsesAuthenticatedTokenSubject() throws Exception {
        UUID subject=UUID.randomUUID();
        when(service.currentUser(subject)).thenReturn(user(subject,Set.of("OPERADOR")));
        mvc.perform(get("/api/auth/me").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt->jwt.subject(subject.toString())).authorities(new SimpleGrantedAuthority("ROLE_OPERADOR"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(subject.toString()));
    }

    @Test void authenticationEndpointIsPublic() throws Exception {
        when(service.login(org.mockito.ArgumentMatchers.any())).thenReturn(new TokenResponse("header.payload.signature","Bearer",Instant.now().plusSeconds(900),user(UUID.randomUUID(),Set.of("OPERADOR"))));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"op@example.com\",\"password\":\"CorrectHorseBattery12\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    private UsuarioResponse user(UUID id,Set<String> roles){return new UsuarioResponse(id,"Operador","op@example.com",EstadoUsuario.ACTIVO,roles,Instant.now(),Instant.now());}
}
