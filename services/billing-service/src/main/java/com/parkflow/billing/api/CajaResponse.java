package com.parkflow.billing.api;

import com.parkflow.billing.domain.Caja;
import java.util.UUID;

public record CajaResponse(UUID id,String codigo,String nombre,String descripcion,boolean activa) {
    public static CajaResponse from(Caja c){return new CajaResponse(c.getId(),c.getCodigo(),c.getNombre(),c.getDescripcion(),c.isActiva());}
}
