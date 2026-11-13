package com.parkflow.identity.api;
import java.util.UUID;
import com.parkflow.identity.domain.Rol;
public record RolResponse(UUID id,String nombre,String descripcion) {
    public static RolResponse from(Rol r){return new RolResponse(r.getId(),r.getNombre(),r.getDescripcion());}
}
