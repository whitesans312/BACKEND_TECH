package com.parkflow.pisos.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "pisos")
public class Piso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo", nullable = false, length = 15)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "numero", nullable = false)
    private Short orden;

    @Column(name = "activo", nullable = false)
    private boolean habilitado = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "actualizado_en", nullable = false)
    private Instant updatedAt;

    protected Piso() {
    }

    public Piso(String codigo, String nombre, short orden) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.orden = orden;
        this.habilitado = true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void updateDetails(String codigo, String nombre, short orden) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.orden = orden;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public Short getOrden() { return orden; }
    public boolean isHabilitado() { return habilitado; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
