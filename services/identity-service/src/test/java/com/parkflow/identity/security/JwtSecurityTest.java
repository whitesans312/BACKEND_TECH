package com.parkflow.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.parkflow.identity.domain.EstadoUsuario;
import com.parkflow.identity.domain.Rol;
import com.parkflow.identity.domain.Usuario;
import com.parkflow.identity.infrastructure.UsuarioRepository;
import static org.mockito.Mockito.*;

class JwtSecurityTest {
    static RSAKey keys,otherKeys; static JwtDecoder decoder; static JwtEncoder encoder,otherEncoder;
    static IdentityJwtProperties properties=new IdentityJwtProperties("https://identity.test","parkflow-api","test-key",java.time.Duration.ofMinutes(15),""," ");
    @BeforeAll static void setup() throws Exception {
        keys=new RSAKeyGenerator(2048).keyID("test-key").generate();otherKeys=new RSAKeyGenerator(2048).keyID("other-key").generate();
        JwtKeyConfiguration config=new JwtKeyConfiguration();decoder=config.jwtDecoder(keys,properties);
        encoder=new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(keys)));
        otherEncoder=new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(otherKeys)));
    }
    @Test void acceptsValidSignatureIssuerAudienceAndExpiry(){
        Jwt jwt=decoder.decode(token(encoder,Instant.now().plusSeconds(60),"parkflow-api"));
        assertThat(jwt.getSubject()).isEqualTo("user-123");
        assertThat(jwt.getClaimAsStringList("roles")).contains("OPERADOR");
    }
    @Test void rejectsExpiredJwt(){assertThatThrownBy(()->decoder.decode(token(encoder,Instant.now().minusSeconds(120),"parkflow-api"))).isInstanceOf(JwtException.class);}
    @Test void rejectsWrongAudience(){assertThatThrownBy(()->decoder.decode(token(encoder,Instant.now().plusSeconds(60),"other-api"))).isInstanceOf(JwtException.class);}
    @Test void rejectsWrongIssuer(){assertThatThrownBy(()->decoder.decode(token(encoder,Instant.now().plusSeconds(60),"https://other.test","parkflow-api"))).isInstanceOf(JwtException.class);}
    @Test void rejectsInvalidSignature(){assertThatThrownBy(()->decoder.decode(token(otherEncoder,Instant.now().plusSeconds(60),"parkflow-api"))).isInstanceOf(JwtException.class);}
    @Test void rejectsAlgorithmOtherThanRs256() throws Exception {
        RSAKey rs384Keys=new RSAKey.Builder(keys.toRSAPublicKey()).privateKey(keys.toRSAPrivateKey()).keyID("test-key").algorithm(com.nimbusds.jose.JWSAlgorithm.RS384).build();
        JwtEncoder rs384Encoder=new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rs384Keys)));
        Instant now=Instant.now();JwtClaimsSet claims=JwtClaimsSet.builder().issuer(properties.issuer()).subject("user-123").audience(List.of("parkflow-api")).issuedAt(now).expiresAt(now.plusSeconds(60)).build();
        String value=rs384Encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS384).keyId("test-key").build(),claims)).getTokenValue();
        assertThatThrownBy(()->decoder.decode(value)).isInstanceOf(JwtException.class);
    }
    @Test void jwksContainsPublicKeyOnly(){
        var response=new com.parkflow.identity.api.JwksController(keys).jwks();
        var jwk=((List<?>)response.get("keys")).get(0);
        assertThat(jwk.toString()).contains("test-key").doesNotContain("\"d\"");
    }
    @Test void jwtRolesBecomeSpringAuthorities(){
        UsuarioRepository repository=mock(UsuarioRepository.class);Usuario user=mock(Usuario.class);Rol role=mock(Rol.class);
        when(user.getEstado()).thenReturn(EstadoUsuario.ACTIVO);when(user.getRoles()).thenReturn(java.util.Set.of(role));when(role.getNombre()).thenReturn("OPERADOR");
        when(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000123"))).thenReturn(java.util.Optional.of(user));
        var jwt=Jwt.withTokenValue("token").header("alg","RS256").subject("00000000-0000-0000-0000-000000000123").claim("roles",List.of("OPERADOR")).build();
        var authentication=new IdentityJwtAuthenticationConverter(repository).convert(jwt);
        assertThat(authentication.getAuthorities()).extracting("authority").contains("ROLE_OPERADOR");
    }
    @Test void rejectsJwtForInactiveUser(){
        UsuarioRepository repository=mock(UsuarioRepository.class);Usuario user=mock(Usuario.class);
        when(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000123"))).thenReturn(java.util.Optional.of(user));
        when(user.getEstado()).thenReturn(EstadoUsuario.INACTIVO);
        var jwt=Jwt.withTokenValue("token").header("alg","RS256").subject("00000000-0000-0000-0000-000000000123").build();
        assertThatThrownBy(()->new IdentityJwtAuthenticationConverter(repository).convert(jwt)).isInstanceOf(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class);
    }
    @Test void loadsPkcs8PrivateAndX509PublicKeyFromExternalBase64Config() throws Exception {
        String privateKey=Base64.getEncoder().encodeToString(keys.toRSAPrivateKey().getEncoded());
        String publicKey=Base64.getEncoder().encodeToString(keys.toRSAPublicKey().getEncoded());
        IdentityJwtProperties external=new IdentityJwtProperties(properties.issuer(),properties.audience(),"external-key",properties.ttl(),privateKey,publicKey);
        RSAKey loaded=new JwtKeyConfiguration().identityRsaKey(external);
        assertThat(loaded.getKeyID()).isEqualTo("external-key");
        assertThat(loaded.toRSAPublicKey().getModulus()).isEqualTo(keys.toRSAPublicKey().getModulus());
    }
    private static String token(JwtEncoder signingEncoder,Instant expires,String audience){
        return token(signingEncoder,expires,properties.issuer(),audience);
    }
    private static String token(JwtEncoder signingEncoder,Instant expires,String issuer,String audience){
        Instant now=Instant.now();Instant issued=expires.isBefore(now)?expires.minusSeconds(60):now.minusSeconds(1);JwtClaimsSet claims=JwtClaimsSet.builder().issuer(issuer).subject("user-123").audience(List.of(audience)).issuedAt(issued).expiresAt(expires).claim("roles",List.of("OPERADOR")).build();
        return signingEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId("test-key").build(),claims)).getTokenValue();
    }
}
