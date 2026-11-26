package com.parkflow.reservas.domain;

import com.parkflow.espacios.domain.Espacio;
import com.parkflow.espacios.domain.TipoEspacio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservas")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo_reserva", nullable = false, length = 30, unique = true)
    private String codigoReserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "espacio_id", nullable = false)
    private Espacio espacio;

    @Column(nullable = false, length = 10)
    private String placa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", nullable = false, length = 10)
    private TipoEspacio tipoVehiculo;

    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private Instant fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoReserva estado = EstadoReserva.ACTIVA;

    @Column(name = "sesion_id")
    private UUID sesionId;

    @Column(name = "clave_idempotencia", nullable = false, unique = true)
    private UUID claveIdempotencia;

    @Column(name = "operador_creacion_id", nullable = false)
    private UUID operadorCreacionId;

    @Column(name = "operador_cierre_id")
    private UUID operadorCierreId;

    @Column(name = "motivo_cancelacion", length = 255)
    private String motivoCancelacion;

    @Column(length = 255)
    private String observacion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Reserva() {}

    public Reserva(String codigoReserva, Espacio espacio, String placa, TipoEspacio tipoVehiculo,
                   Instant fechaInicio, Instant fechaFin, UUID claveIdempotencia, UUID operadorCreacionId,
                   String observacion) {
        this.codigoReserva = codigoReserva;
        this.espacio = espacio;
        this.placa = placa;
        this.tipoVehiculo = tipoVehiculo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.claveIdempotencia = claveIdempotencia;
        this.operadorCreacionId = operadorCreacionId;
        this.observacion = observacion;
    }

    public void convertir(UUID sesionId, UUID operadorId, Instant now) {
        if (estado != EstadoReserva.ACTIVA) {
            throw new IllegalStateException("Solo se puede hacer check-in de una reserva activa.");
        }
        this.sesionId = sesionId;
        this.operadorCierreId = operadorId;
        this.estado = EstadoReserva.CONVERTIDA;
        this.actualizadoEn = now;
    }

    public void cancelar(UUID operadorId, String motivo, Instant now) {
        if (estado != EstadoReserva.ACTIVA) {
            throw new IllegalStateException("Solo se puede cancelar una reserva activa.");
        }
        this.operadorCierreId = operadorId;
        this.motivoCancelacion = motivo;
        this.estado = EstadoReserva.CANCELADA;
        this.actualizadoEn = now;
    }

    public void expirar(Instant now) {
        if (estado == EstadoReserva.ACTIVA) {
            this.estado = EstadoReserva.EXPIRADA;
            this.actualizadoEn = now;
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (creadoEn == null) {
            creadoEn = now;
        }
        if (actualizadoEn == null) {
            actualizadoEn = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        actualizadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public String getCodigoReserva() { return codigoReserva; }
    public Espacio getEspacio() { return espacio; }
    public String getPlaca() { return placa; }
    public TipoEspacio getTipoVehiculo() { return tipoVehiculo; }
    public Instant getFechaInicio() { return fechaInicio; }
    public Instant getFechaFin() { return fechaFin; }
    public EstadoReserva getEstado() { return estado; }
    public UUID getSesionId() { return sesionId; }
    public UUID getClaveIdempotencia() { return claveIdempotencia; }
    public UUID getOperadorCreacionId() { return operadorCreacionId; }
    public UUID getOperadorCierreId() { return operadorCierreId; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public String getObservacion() { return observacion; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
