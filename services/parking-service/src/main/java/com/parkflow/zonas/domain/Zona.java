package com.parkflow.zonas.domain;

import com.parkflow.pisos.domain.Piso;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "zonas")
public class Zona {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "piso_id", nullable = false) private Piso piso;
    @Column(nullable = false, length = 15) private String codigo;
    @Column(nullable = false, length = 80) private String nombre;
    @Column private String descripcion;
    @Column(nullable = false) private boolean activa = true;
    @Column(name = "creado_en", nullable = false, updatable = false) private Instant creadoEn;
    @Column(name = "actualizado_en", nullable = false) private Instant actualizadoEn;
    protected Zona() {}
    public Zona(Piso piso, String codigo, String nombre, String descripcion) {
        this.piso = piso; this.codigo = codigo; this.nombre = nombre; this.descripcion = descripcion;
    }
    @PrePersist void insertTime() { creadoEn = actualizadoEn = Instant.now(); }
    @PreUpdate void updateTime() { actualizadoEn = Instant.now(); }
    public UUID getId() { return id; }
    public Piso getPiso() { return piso; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public boolean isActiva() { return activa; }
}
