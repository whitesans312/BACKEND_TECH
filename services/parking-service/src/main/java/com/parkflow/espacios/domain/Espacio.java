package com.parkflow.espacios.domain;

import com.parkflow.zonas.domain.Zona;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "espacios")
public class Espacio {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "zona_id", nullable = false) private Zona zona;
    @Column(nullable = false, length = 20) private String codigo;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_vehiculo", nullable = false, length = 10) private TipoEspacio tipo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EstadoEspacio estado = EstadoEspacio.HABILITADO;
    @Column private String descripcion;
    @Column(name = "creado_en", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "actualizado_en", nullable = false) private Instant updatedAt;
    protected Espacio() {}
    public Espacio(Zona zona, String codigo, TipoEspacio tipo) { this.zona=zona; this.codigo=codigo; this.tipo=tipo; }
    @PrePersist void onCreate() { createdAt=updatedAt=Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt=Instant.now(); }
    public void updateDetails(Zona zona,String codigo,TipoEspacio tipo) { this.zona=zona; this.codigo=codigo; this.tipo=tipo; }
    public void setHabilitado(boolean enabled) { estado=enabled?EstadoEspacio.HABILITADO:EstadoEspacio.DESHABILITADO; }
    public UUID getId(){return id;} public Zona getZona(){return zona;} public TipoEspacio getTipo(){return tipo;}
    public String getCodigo(){return codigo;} public boolean isHabilitado(){return estado==EstadoEspacio.HABILITADO;}
}
