package com.parkflow.billing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;

@Entity @Table(name="tarifas")
public class Tarifa {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Enumerated(EnumType.STRING) @Column(name="tipo_vehiculo",nullable=false,length=10) private TipoVehiculo tipoVehiculo;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=25) private ConceptoTarifa concepto;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal valor;
    @JdbcTypeCode(Types.CHAR) @Column(nullable=false,length=3,columnDefinition="char(3)") private String moneda;
    @Column(name="vigente_desde",nullable=false) private Instant vigenteDesde;
    @Column(name="vigente_hasta") private Instant vigenteHasta;
    @Column(nullable=false) private boolean activa=true;
    @Column(name="creado_por") private UUID creadoPor;
    @Column(name="creado_en",nullable=false,updatable=false) private Instant creadoEn;
    protected Tarifa(){}
    public Tarifa(TipoVehiculo tipo, ConceptoTarifa concepto, BigDecimal valor, String moneda, Instant inicio) {
        this.tipoVehiculo=tipo; this.concepto=concepto; this.valor=valor; this.moneda=moneda; this.vigenteDesde=inicio; this.activa=true;
    }
    @PrePersist void createTime(){if(creadoEn==null)creadoEn=Instant.now();}
    public void cerrarVigencia(Instant hasta){if(hasta.isBefore(vigenteDesde))throw new IllegalArgumentException("Vigencia inválida");vigenteHasta=hasta;activa=false;}
    public UUID getId(){return id;} public TipoVehiculo getTipoVehiculo(){return tipoVehiculo;} public ConceptoTarifa getConcepto(){return concepto;}
    public BigDecimal getValor(){return valor;} public String getMoneda(){return moneda;} public Instant getVigenteDesde(){return vigenteDesde;}
    public Instant getVigenteHasta(){return vigenteHasta;} public boolean isActiva(){return activa;} public Instant getCreatedAt(){return creadoEn;}
    public short getUnidadMinutos(){return 60;} public short getMinutosGracia(){return 15;} public BigDecimal getValorUnidad(){return valor;}
}
