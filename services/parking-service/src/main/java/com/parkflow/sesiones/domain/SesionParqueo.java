package com.parkflow.sesiones.domain;

import java.time.Instant;
import java.util.UUID;
import com.parkflow.espacios.domain.Espacio;
import jakarta.persistence.*;

@Entity @Table(name = "sesiones_parqueo")
public class SesionParqueo {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name="numero_tiquete", nullable=false, length=30, unique=true) private String numeroTicket;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="espacio_id", nullable=false) private Espacio espacio;
    @Column(name="placa_ingreso", nullable=false, length=10, updatable=false) private String placaIngreso;
    @Column(name="placa_actual", nullable=false, length=10) private String placaActual;
    @Enumerated(EnumType.STRING) @Column(name="tipo_vehiculo", nullable=false, length=10) private com.parkflow.espacios.domain.TipoEspacio tipoVehiculo;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private EstadoSesion estado = EstadoSesion.ACTIVA;
    @Column(name="hora_entrada", nullable=false) private Instant fechaEntrada;
    @Column(name="hora_salida") private Instant fechaSalida;
    @Column(name="tipo_cierre", length=15) private String tipoCierre;
    @Column(name="liquidacion_id") private UUID liquidacionId;
    @Column(name="operacion_cierre_id", unique=true) private UUID operacionCierreId;
    @Column(name="clave_idempotencia", unique=true) private UUID claveIdempotencia;
    @Column(name="operador_entrada_id", nullable=false) private UUID operadorEntradaId;
    @Column(name="operador_salida_id") private UUID operadorCierreId;
    @Column(name="creado_en", nullable=false, updatable=false) private Instant createdAt;
    @Column(name="actualizado_en", nullable=false) private Instant updatedAt;
    protected SesionParqueo() {}
    public SesionParqueo(String ticket, String placa, com.parkflow.espacios.domain.TipoEspacio tipo, Espacio espacio, UUID operador) { this.numeroTicket=ticket; this.placaIngreso=placa; this.placaActual=placa; this.tipoVehiculo=tipo; this.espacio=espacio; this.operadorEntradaId=operador; this.fechaEntrada=Instant.now(); }
    public void setClaveIdempotencia(UUID key){this.claveIdempotencia=key;}
    public UUID getClaveIdempotencia(){return claveIdempotencia;}
    public void cerrar(UUID liquidacionId, UUID operacionId, UUID operadorId, Instant salida, String tipo) {
        if (estado != EstadoSesion.ACTIVA) throw new IllegalStateException("La sesión ya está cerrada.");
        if (salida.isBefore(fechaEntrada)) throw new IllegalArgumentException("La hora de salida no puede ser anterior al ingreso.");
        this.liquidacionId=liquidacionId; this.operacionCierreId=operacionId; this.operadorCierreId=operadorId;
        this.fechaSalida=salida; this.tipoCierre=tipo; this.estado=EstadoSesion.CERRADA;
    }
    public void corregirPlaca(String placa) {
        if (estado != EstadoSesion.ACTIVA) throw new IllegalStateException("Solo se puede corregir la placa de una sesión activa.");
        this.placaActual=placa;
    }
    @PrePersist void createTimes() { createdAt=updatedAt=Instant.now(); }
    @PreUpdate void updateTime() { updatedAt=Instant.now(); }
    public UUID getId(){return id;} public String getNumeroTicket(){return numeroTicket;} public String getPlacaActual(){return placaActual;} public com.parkflow.espacios.domain.TipoEspacio getTipoVehiculo(){return tipoVehiculo;} public Espacio getEspacio(){return espacio;} public EstadoSesion getEstado(){return estado;} public Instant getFechaEntrada(){return fechaEntrada;} public Instant getFechaSalida(){return fechaSalida;} public UUID getOperadorEntradaId(){return operadorEntradaId;}
    public UUID getOperacionCierreId(){return operacionCierreId;}
}
