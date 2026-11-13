package com.parkflow.billing.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="cajas")
public class Caja {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(nullable=false,unique=true,length=20) private String codigo;
    @Column(nullable=false,length=80) private String nombre;
    private String descripcion;
    @Column(nullable=false) private boolean activa=true;
    @Column(name="creado_en",nullable=false,updatable=false) private Instant creadoEn;
    @Column(name="actualizado_en",nullable=false) private Instant actualizadoEn;
    protected Caja(){}
    public Caja(String codigo,String nombre,String descripcion){this.codigo=codigo;this.nombre=nombre;this.descripcion=descripcion;}
    @PrePersist void create(){creadoEn=actualizadoEn=Instant.now();}
    @PreUpdate void update(){actualizadoEn=Instant.now();}
    public UUID getId(){return id;} public String getCodigo(){return codigo;} public String getNombre(){return nombre;}
    public String getDescripcion(){return descripcion;} public boolean isActiva(){return activa;}
}
