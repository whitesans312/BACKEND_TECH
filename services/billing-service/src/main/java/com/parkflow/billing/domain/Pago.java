package com.parkflow.billing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pagos")
public class Pago {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "liquidacion_id", nullable = false) private Liquidacion liquidacion;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal monto;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15) private MetodoPago metodo = MetodoPago.EFECTIVO;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15) private EstadoPago estado = EstadoPago.PENDIENTE;
    @Column(name = "operador_id", nullable = false) private UUID operadorId;
    @Column(name = "sesion_caja_id") private UUID sesionCajaId;
    @Column(name = "clave_idempotencia", nullable = false, unique = true) private UUID claveIdempotencia;
    @Column(name = "aprobado_en") private Instant aprobadoEn;
    @Column(name = "rechazado_en") private Instant rechazadoEn;
    @Column(name = "resuelto_por") private UUID resueltoPor;
    @Column(name = "motivo_rechazo") private String motivoRechazo;
    @Column(name = "creado_en", nullable = false, updatable = false) private Instant creadoEn;

    protected Pago() { }
    public Pago(Liquidacion liquidacion, UUID claveIdempotencia, UUID operadorId) {
        this.liquidacion = liquidacion;
        this.monto = liquidacion.getTotal();
        this.claveIdempotencia = claveIdempotencia;
        this.operadorId = operadorId;
    }
    @PrePersist void onCreate() { if (creadoEn == null) creadoEn = Instant.now(); }
    public void approve(UUID sesionCajaId, UUID operatorId, Instant when) {
        if (estado != EstadoPago.PENDIENTE) throw new IllegalStateException("Solo se puede aprobar un pago pendiente.");
        this.sesionCajaId = sesionCajaId;
        this.aprobadoEn = when;
        this.resueltoPor = operatorId;
        this.estado = EstadoPago.APROBADO;
    }
    public void reject(UUID operatorId, String reason, Instant when) {
        if (estado != EstadoPago.PENDIENTE) throw new IllegalStateException("Solo se puede rechazar un pago pendiente.");
        this.estado = EstadoPago.RECHAZADO;
        this.resueltoPor = operatorId;
        this.motivoRechazo = reason;
        this.rechazadoEn = when;
    }
    public UUID getId() { return id; }
    public Liquidacion getLiquidacion() { return liquidacion; }
    public BigDecimal getMonto() { return monto; }
    public MetodoPago getMetodo() { return metodo; }
    public EstadoPago getEstado() { return estado; }
    public UUID getOperadorId() { return operadorId; }
    public UUID getSesionCajaId() { return sesionCajaId; }
    public UUID getClaveIdempotencia() { return claveIdempotencia; }
    public Instant getAprobadoEn() { return aprobadoEn; }
    public UUID getResueltoPor() { return resueltoPor; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public Instant getCreadoEn() { return creadoEn; }
}
