package com.parkflow.identity.security;

import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import com.parkflow.identity.domain.EstadoUsuario;
import com.parkflow.identity.infrastructure.UsuarioRepository;

/** Revalida estado y roles locales en Identity en cada petición con bearer token. */
@Component
public class IdentityJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UsuarioRepository usuarios;
    public IdentityJwtAuthenticationConverter(UsuarioRepository usuarios){this.usuarios=usuarios;}
    @Override public AbstractAuthenticationToken convert(Jwt jwt){
        final UUID userId;
        try { userId=UUID.fromString(jwt.getSubject()); }
        catch(IllegalArgumentException|NullPointerException e){throw invalidToken();}
        var user=usuarios.findById(userId).orElseThrow(IdentityJwtAuthenticationConverter::invalidToken);
        if(user.getEstado()!=EstadoUsuario.ACTIVO) throw invalidToken();
        var authorities=user.getRoles().stream().map(role->new SimpleGrantedAuthority("ROLE_"+role.getNombre())).toList();
        return new JwtAuthenticationToken(jwt,authorities,userId.toString());
    }
    private static OAuth2AuthenticationException invalidToken(){return new OAuth2AuthenticationException(new OAuth2Error("invalid_token","El usuario asociado al token no está activo o no existe.",null));}
}
