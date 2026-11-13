package com.parkflow.billing.application;

import com.parkflow.billing.api.AbrirSesionCajaRequest;
import com.parkflow.billing.api.CajaResponse;
import com.parkflow.billing.api.CerrarSesionCajaRequest;
import com.parkflow.billing.api.CrearCajaRequest;
import com.parkflow.billing.api.SesionCajaResponse;
import com.parkflow.billing.domain.Caja;
import com.parkflow.billing.domain.SesionCaja;
import com.parkflow.billing.infrastructure.CajaRepository;
import com.parkflow.billing.infrastructure.SesionCajaRepository;
import com.parkflow.billing.shared.ConflictException;
import com.parkflow.billing.shared.ResourceNotFoundException;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CajaService {
    private final CajaRepository cajas;
    private final SesionCajaRepository sesiones;
    private final Clock clock;
    @Autowired public CajaService(CajaRepository cajas,SesionCajaRepository sesiones){this(cajas,sesiones,Clock.systemUTC());}
    CajaService(CajaRepository cajas,SesionCajaRepository sesiones,Clock clock){this.cajas=cajas;this.sesiones=sesiones;this.clock=clock;}

    @Transactional(readOnly=true)
    public List<CajaResponse> listar(){return cajas.findAllByOrderByCodigoAsc().stream().map(CajaResponse::from).toList();}

    @Transactional
    public CajaResponse crear(CrearCajaRequest request){
        String code=request.codigo().trim().toUpperCase(Locale.ROOT);
        if(cajas.existsByCodigoIgnoreCase(code))throw new ConflictException("Ya existe una caja con ese código.");
        return CajaResponse.from(cajas.saveAndFlush(new Caja(code,request.nombre().trim(),request.descripcion())));
    }

    @Transactional
    public SesionCajaResponse abrir(UUID key,AbrirSesionCajaRequest request,UUID operatorId){
        var prior=sesiones.findByClaveIdempotencia(key);
        if(prior.isPresent()){
            SesionCaja s=prior.get();
            if(!s.getCajaId().equals(request.cajaId())||!s.getOperadorId().equals(operatorId)
                    ||s.getSaldoInicial().compareTo(request.saldoInicial())!=0)
                throw new ConflictException("La clave de apertura ya se usó con otros datos.");
            return SesionCajaResponse.from(s);
        }
        if(!cajas.findByIdAndActivaTrue(request.cajaId()).isPresent())throw new ResourceNotFoundException("No existe una caja activa con ese identificador.");
        var amount=request.saldoInicial().setScale(2,RoundingMode.UNNECESSARY);
        var now=clock.instant();
        return SesionCajaResponse.from(sesiones.saveAndFlush(new SesionCaja(request.cajaId(),operatorId,amount,request.observacion(),key,now)));
    }

    @Transactional
    public SesionCajaResponse cerrar(UUID id,CerrarSesionCajaRequest request,UUID operatorId){
        SesionCaja s=sesiones.lockById(id).orElseThrow(()->new ResourceNotFoundException("No existe la sesión de caja."));
        String observation=request.observacion()==null?null:request.observacion().trim();
        if(!s.getOperadorId().equals(operatorId))throw new ConflictException("Solo el operador de apertura puede cerrar esta caja.");
        if(!s.isAbierta()){
            if(s.getSaldoContado().compareTo(request.saldoContado())==0
                    && java.util.Objects.equals(s.getObservacionCierre(),observation)) return SesionCajaResponse.from(s);
            throw new ConflictException("La sesión ya está cerrada con otro arqueo.");
        }
        s.cerrar(request.saldoContado().setScale(2,RoundingMode.UNNECESSARY),observation,clock.instant());
        return SesionCajaResponse.from(sesiones.saveAndFlush(s));
    }

    @Transactional(readOnly=true)
    public SesionCajaResponse porId(UUID id){return sesiones.findById(id).map(SesionCajaResponse::from)
            .orElseThrow(()->new ResourceNotFoundException("No existe la sesión de caja."));}
}
