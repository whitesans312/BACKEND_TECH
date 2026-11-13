package com.parkflow.billing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;

@Entity @Table(name="liquidaciones")
public class Liquidacion {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="sesion_parqueo_id") private UUID sesionParqueoId;
    @Column(name="cargo_abandono_id") private UUID cargoAbandonoId;
    @Column(name="numero_tiquete",length=30) private String numeroTiquete;
    @Column(length=10) private String placa;
    @Enumerated(EnumType.STRING) @Column(name="tipo_vehiculo",length=10) private TipoVehiculo tipoVehiculo;
    @Column(name="hora_entrada") private Instant horaEntrada;
    @Column(name="hora_corte",nullable=false) private Instant horaCorte;
    @Column(name="minutos_totales") private Integer minutosTotales;
    @Column(name="minutos_gracia") private Short minutosGracia;
    @Column(name="minutos_facturables") private Integer minutosFacturables;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal subtotal;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal recargos;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal descuentos;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal total;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=15) private EstadoLiquidacion estado;
    @Column(name="clave_idempotencia",nullable=false,unique=true) private UUID claveIdempotencia;
    @JdbcTypeCode(Types.CHAR) @Column(name="hash_solicitud",nullable=false,length=64,columnDefinition="char(64)") private String hashSolicitud;
    @Column(name="operador_liquida_id",nullable=false) private UUID operadorLiquidaId;
    @Column(name="cancelada_por") private UUID canceladaPor;
    @Column(name="motivo_cierre") private String motivoCierre;
    @Column(name="resuelta_en") private Instant resueltaEn;
    @Column(name="creado_en",nullable=false,updatable=false) private Instant creadoEn;
    @Column(name="actualizado_en",nullable=false) private Instant actualizadoEn;
    protected Liquidacion(){}
    public Liquidacion(UUID sesionId,String ticket,String placa,TipoVehiculo tipo,Instant entrada,Instant corte,
                       int minutos,short gracia,int facturables,BigDecimal total,UUID key,String hash,UUID operador) {
        this.sesionParqueoId=sesionId;this.numeroTiquete=ticket;this.placa=placa;this.tipoVehiculo=tipo;
        this.horaEntrada=entrada;this.horaCorte=corte;this.minutosTotales=minutos;this.minutosGracia=gracia;
        this.minutosFacturables=facturables;this.subtotal=total;this.recargos=BigDecimal.ZERO.setScale(2);
        this.descuentos=BigDecimal.ZERO.setScale(2);this.total=total;this.estado=total.signum()==0?EstadoLiquidacion.GRATUITA:EstadoLiquidacion.PENDIENTE;
        this.claveIdempotencia=key;this.hashSolicitud=hash;this.operadorLiquidaId=operador;
    }
    @PrePersist void timestamps(){Instant n=Instant.now();creadoEn=actualizadoEn=n;}
    @PreUpdate void updated(){actualizadoEn=Instant.now();}
    public void marcarPagada(Instant when){if(estado!=EstadoLiquidacion.PENDIENTE)throw new IllegalStateException("Solo una liquidación pendiente puede pagarse.");estado=EstadoLiquidacion.PAGADA;resueltaEn=when;}
    public void cancelar(UUID actor,String motivo,Instant when){if(estado!=EstadoLiquidacion.PENDIENTE)throw new IllegalStateException("Solo una liquidación pendiente puede cancelarse.");estado=EstadoLiquidacion.CANCELADA;canceladaPor=actor;motivoCierre=motivo;resueltaEn=when;}
    public UUID getId(){return id;} public UUID getSesionParqueoId(){return sesionParqueoId;} public UUID getCargoAbandonoId(){return cargoAbandonoId;}
    public String getNumeroTiquete(){return numeroTiquete;} public String getPlaca(){return placa;} public TipoVehiculo getTipoVehiculo(){return tipoVehiculo;}
    public Instant getHoraEntrada(){return horaEntrada;} public Instant getHoraCorte(){return horaCorte;} public Integer getMinutosTotales(){return minutosTotales;}
    public Short getMinutosGracia(){return minutosGracia;} public Integer getMinutosFacturables(){return minutosFacturables;}
    public BigDecimal getSubtotal(){return subtotal;} public BigDecimal getRecargos(){return recargos;} public BigDecimal getDescuentos(){return descuentos;}
    public BigDecimal getTotal(){return total;} public EstadoLiquidacion getEstado(){return estado;} public UUID getClaveIdempotencia(){return claveIdempotencia;}
    public String getHashSolicitud(){return hashSolicitud;} public UUID getOperadorLiquidaId(){return operadorLiquidaId;} public UUID getCanceladaPor(){return canceladaPor;} public String getMotivoCierre(){return motivoCierre;} public Instant getCreadoEn(){return creadoEn;}
}
