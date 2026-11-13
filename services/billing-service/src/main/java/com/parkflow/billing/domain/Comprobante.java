package com.parkflow.billing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "comprobantes")
public class Comprobante {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "pago_id", unique = true) private Pago pago;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "liquidacion_id", nullable = false, unique = true) private Liquidacion liquidacion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TipoComprobante tipo;
    @Column(nullable = false, length = 30, unique = true) private String numero;
    @Column(name = "numero_tiquete", length = 30) private String numeroTiquete;
    @Column(length = 10) private String placa;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_vehiculo", length = 10) private TipoVehiculo tipoVehiculo;
    @Enumerated(EnumType.STRING) @Column(name = "metodo_pago", length = 15) private MetodoPago metodoPago;
    @Column(name = "hora_entrada") private Instant horaEntrada;
    @Column(name = "hora_corte", nullable = false) private Instant horaCorte;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subtotal;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal recargos;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal descuentos;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total;
    @Column(name = "emitido_en", nullable = false, updatable = false) private Instant emitidoEn;

    protected Comprobante() { }
    public Comprobante(Liquidacion l, Pago p, TipoComprobante tipo, String numero, Instant when) {
        this.liquidacion=l; this.pago=p; this.tipo=tipo; this.numero=numero;
        this.numeroTiquete=l.getNumeroTiquete(); this.placa=l.getPlaca(); this.tipoVehiculo=l.getTipoVehiculo();
        this.metodoPago=p == null ? null : p.getMetodo(); this.horaEntrada=l.getHoraEntrada(); this.horaCorte=l.getHoraCorte();
        this.subtotal=l.getSubtotal(); this.recargos=l.getRecargos(); this.descuentos=l.getDescuentos(); this.total=l.getTotal(); this.emitidoEn=when;
    }
    public UUID getId(){return id;} public Pago getPago(){return pago;} public Liquidacion getLiquidacion(){return liquidacion;}
    public TipoComprobante getTipo(){return tipo;} public String getNumero(){return numero;} public Instant getEmitidoEn(){return emitidoEn;}
}
