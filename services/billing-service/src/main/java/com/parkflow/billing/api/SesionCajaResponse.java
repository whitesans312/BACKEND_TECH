package com.parkflow.billing.api;

import com.parkflow.billing.domain.SesionCaja;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SesionCajaResponse(UUID id,UUID cajaId,UUID operadorId,String estado,BigDecimal saldoInicial,
                                 BigDecimal saldoEsperado,BigDecimal saldoContado,BigDecimal diferencia,
                                 Instant abiertaEn,Instant cerradaEn,String observacionApertura,String observacionCierre) {
    public static SesionCajaResponse from(SesionCaja s){return new SesionCajaResponse(s.getId(),s.getCajaId(),s.getOperadorId(),s.getEstado(),s.getSaldoInicial(),s.getSaldoEsperado(),s.getSaldoContado(),s.getDiferencia(),s.getAbiertaEn(),s.getCerradaEn(),s.getObservacionApertura(),s.getObservacionCierre());}
}
