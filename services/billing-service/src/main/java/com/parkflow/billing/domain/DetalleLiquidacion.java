package com.parkflow.billing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="detalles_liquidacion")
public class DetalleLiquidacion {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="liquidacion_id",nullable=false) private UUID liquidacionId;
    @Column(name="tarifa_id") private UUID tarifaId;
    @Column(nullable=false,length=30) private String concepto;
    @Column(nullable=false,length=200) private String descripcion;
    @Column(nullable=false,precision=10,scale=2) private BigDecimal cantidad;
    @Column(name="valor_unitario",nullable=false,precision=12,scale=2) private BigDecimal valorUnitario;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal subtotal;
    @Column(name="creado_en",nullable=false,updatable=false) private Instant creadoEn;
    protected DetalleLiquidacion(){}
    public DetalleLiquidacion(UUID liq,UUID tarifa,String concepto,String descripcion,BigDecimal cantidad,BigDecimal valor,BigDecimal subtotal){this.liquidacionId=liq;this.tarifaId=tarifa;this.concepto=concepto;this.descripcion=descripcion;this.cantidad=cantidad;this.valorUnitario=valor;this.subtotal=subtotal;}
    @PrePersist void timestamp(){creadoEn=Instant.now();}
}
