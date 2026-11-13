package com.parkflow.identity.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity @Table(name="roles")
public class Rol {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(nullable=false, unique=true, length=30) private String nombre;
    @Column(length=150) private String descripcion;
    @Column(name="creado_en", nullable=false, updatable=false) private Instant createdAt;
    protected Rol() {}
    @PrePersist void onCreate() { createdAt=Instant.now(); }
    public UUID getId(){return id;} public String getNombre(){return nombre;} public String getDescripcion(){return descripcion;}
}
