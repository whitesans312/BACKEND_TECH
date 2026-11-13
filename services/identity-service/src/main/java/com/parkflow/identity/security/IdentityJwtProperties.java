package com.parkflow.identity.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="identity.jwt")
public record IdentityJwtProperties(String issuer,String audience,String keyId,Duration ttl,String privateKeyB64,String publicKeyB64) {}
