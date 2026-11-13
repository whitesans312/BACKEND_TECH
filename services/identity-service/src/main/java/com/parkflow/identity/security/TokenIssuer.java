package com.parkflow.identity.security;

import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.stereotype.Component;
import com.parkflow.identity.application.IdentityService.IssuedToken;
import com.parkflow.identity.domain.Usuario;

@Component
public class TokenIssuer {
    private final JwtEncoder encoder; private final IdentityJwtProperties properties;
    public TokenIssuer(JwtEncoder encoder,IdentityJwtProperties properties){this.encoder=encoder;this.properties=properties;}
    public IssuedToken issue(Usuario user){
        Instant now=Instant.now(); Instant expires=now.plus(properties.ttl());
        List<String> roleNames=user.getRoles().stream().map(r->r.getNombre()).sorted().toList();
        JwtClaimsSet claims=JwtClaimsSet.builder().issuer(properties.issuer()).subject(user.getId().toString()).audience(List.of(properties.audience())).issuedAt(now).expiresAt(expires).claim("roles",roleNames).build();
        JwsHeader header=JwsHeader.with(SignatureAlgorithm.RS256).keyId(properties.keyId()).build();
        return new IssuedToken(encoder.encode(JwtEncoderParameters.from(header,claims)).getTokenValue(),expires);
    }
}
