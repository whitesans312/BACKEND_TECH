package com.parkflow.billing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="sesiones_caja")
public class SesionCaja {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="caja_id",nullable=false) private UUID cajaId;
    @Column(name="operador_id",nullable=false) private UUID operadorId;
    @Column(nullable=false,length=10) private String estado;
    @Column(name="saldo_inicial",nullable=false,precision=12,scale=2) private BigDecimal saldoInicial;
    @Column(name="saldo_esperado",nullable=false,precision=12,scale=2) private BigDecimal saldoEsperado;
    @Column(name="saldo_contado",precision=12,scale=2) private BigDecimal saldoContado;
    @Column(precision=12,scale=2) private BigDecimal diferencia;
    @Column(name="abierta_en",nullable=false) private Instant abiertaEn;
    @Column(name="cerrada_en") private Instant cerradaEn;
    @Column(name="observacion_apertura") private String observacionApertura;
    @Column(name="observacion_cierre") private String observacionCierre;
    @Column(name="clave_idempotencia",nullable=false,unique=true) private UUID claveIdempotencia;
    @Column(name="creado_en",nullable=false) private Instant creadoEn;
    @Column(name="actualizado_en",nullable=false) private Instant actualizadoEn;
    protected SesionCaja(){}
    public SesionCaja(UUID cajaId,UUID operadorId,BigDecimal saldoInicial,String observacion,UUID key,Instant now){
        this.cajaId=cajaId;this.operadorId=operadorId;this.saldoInicial=saldoInicial;this.saldoEsperado=saldoInicial;
        this.observacionApertura=observacion;this.claveIdempotencia=key;this.estado="ABIERTA";this.abiertaEn=now;
        this.creadoEn=now;this.actualizadoEn=now;
    }
    public void cerrar(BigDecimal contado,String observacion,Instant now){
        if(!isAbierta())throw new IllegalStateException("La sesión de caja ya está cerrada.");
        this.estado="CERRADA";this.saldoContado=contado;this.diferencia=contado.subtract(saldoEsperado);
        this.observacionCierre=observacion;this.cerradaEn=now;this.actualizadoEn=now;
    }
    public UUID getId(){return id;} public UUID getOperadorId(){return operadorId;} public String getEstado(){return estado;}
    public UUID getCajaId(){return cajaId;} public UUID getClaveIdempotencia(){return claveIdempotencia;}
    public BigDecimal getSaldoInicial(){return saldoInicial;} public BigDecimal getSaldoEsperado(){return saldoEsperado;}
    public BigDecimal getSaldoContado(){return saldoContado;} public BigDecimal getDiferencia(){return diferencia;}
    public Instant getAbiertaEn(){return abiertaEn;} public Instant getCerradaEn(){return cerradaEn;}
    public String getObservacionApertura(){return observacionApertura;} public String getObservacionCierre(){return observacionCierre;}
    public boolean isAbierta(){return "ABIERTA".equals(estado);}
}
