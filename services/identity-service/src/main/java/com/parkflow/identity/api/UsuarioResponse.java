package com.parkflow.identity.api;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.parkflow.identity.domain.EstadoUsuario;
import com.parkflow.identity.domain.Usuario;

public record UsuarioResponse(UUID id,String nombre,String email,EstadoUsuario estado,Set<String> roles,Instant createdAt,Instant updatedAt) {
    public static UsuarioResponse from(Usuario u){return new UsuarioResponse(u.getId(),u.getNombre(),u.getEmail(),u.getEstado(),u.getRoles().stream().map(r->r.getNombre()).collect(Collectors.toUnmodifiableSet()),u.getCreatedAt(),u.getUpdatedAt());}
}
