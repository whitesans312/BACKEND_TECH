package com.parkflow.identity.api;
import java.time.Instant;
public record TokenResponse(String accessToken,String tokenType,Instant expiresAt,UsuarioResponse usuario) {}
