package com.parkflow.identity.security;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

@Configuration
@EnableConfigurationProperties(IdentityJwtProperties.class)
public class JwtKeyConfiguration {
    @Bean RSAKey identityRsaKey(IdentityJwtProperties p) {
        try {
            if(p.issuer()==null||p.issuer().isBlank()||p.audience()==null||p.audience().isBlank()||p.keyId()==null||p.keyId().isBlank()||p.ttl()==null||p.ttl().isZero()||p.ttl().isNegative()) throw new IllegalStateException("issuer, audience, key-id y ttl JWT deben configurarse con valores válidos.");
            if(p.privateKeyB64()==null||p.privateKeyB64().isBlank()||p.publicKeyB64()==null||p.publicKeyB64().isBlank()) throw new IllegalStateException("Configure IDENTITY_JWT_PRIVATE_KEY_B64 y IDENTITY_JWT_PUBLIC_KEY_B64 con claves externas RSA PKCS#8/X.509.");
            KeyFactory factory=KeyFactory.getInstance("RSA");
            byte[] privateBytes=Base64.getDecoder().decode(p.privateKeyB64()); byte[] publicBytes=Base64.getDecoder().decode(p.publicKeyB64());
            RSAPrivateKey privateKey=(RSAPrivateKey)factory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
            RSAPublicKey publicKey=(RSAPublicKey)factory.generatePublic(new X509EncodedKeySpec(publicBytes));
            if(privateKey.getModulus().bitLength()<2048||publicKey.getModulus().bitLength()<2048) throw new IllegalStateException("Las claves JWT RSA deben tener al menos 2048 bits.");
            boolean publicExponentMatches=!(privateKey instanceof RSAPrivateCrtKey privateCrt)||privateCrt.getPublicExponent().equals(publicKey.getPublicExponent());
            if(!privateKey.getModulus().equals(publicKey.getModulus())||!publicExponentMatches) throw new IllegalStateException("La clave privada y pública JWT no forman el mismo par RSA.");
            return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(p.keyId()).algorithm(com.nimbusds.jose.JWSAlgorithm.RS256).build();
        } catch(IllegalStateException e){throw e;} catch(Exception e){throw new IllegalStateException("No fue posible cargar las claves RSA JWT configuradas.",e);}
    }
    @Bean JwtEncoder jwtEncoder(RSAKey key){return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));}
    @Bean JwtDecoder jwtDecoder(RSAKey key,IdentityJwtProperties p){
        try {
            NimbusJwtDecoder decoder=NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).signatureAlgorithm(SignatureAlgorithm.RS256).build();
            OAuth2TokenValidator<Jwt> issuer=JwtValidators.createDefaultWithIssuer(p.issuer());
            OAuth2TokenValidator<Jwt> audience=jwt->jwt.getAudience().contains(p.audience())?org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success():org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(new org.springframework.security.oauth2.core.OAuth2Error("invalid_token","Required audience is missing.",null));
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer,audience));
            return decoder;
        } catch(Exception e){throw new IllegalStateException("No fue posible configurar el validador JWT.",e);}
    }
}
