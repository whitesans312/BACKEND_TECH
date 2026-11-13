package com.parkflow.billing.application;

import com.parkflow.billing.api.ActualizarTarifaRequest;
import com.parkflow.billing.api.TarifaResponse;
import com.parkflow.billing.domain.ConceptoTarifa;
import com.parkflow.billing.domain.Tarifa;
import com.parkflow.billing.domain.TipoVehiculo;
import com.parkflow.billing.infrastructure.TarifaRepository;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TarifaService {
    private final TarifaRepository tarifas;
    public TarifaService(TarifaRepository tarifas) { this.tarifas=tarifas; }

    @Transactional(readOnly=true)
    public List<TarifaResponse> listar(TipoVehiculo tipo) {
        List<Tarifa> found=tipo==null?tarifas.findAllByOrderByTipoVehiculoAscConceptoAscVigenteDesdeDesc():tarifas.findByTipoVehiculoOrderByVigenteDesdeDesc(tipo);
        return found.stream().map(TarifaResponse::from).toList();
    }

    @Transactional
    public TarifaResponse reemplazarVigente(TipoVehiculo tipo, ConceptoTarifa concepto, ActualizarTarifaRequest request) {
        Instant now=Instant.now();
        tarifas.lockActive(tipo,concepto).ifPresent(old->{old.cerrarVigencia(now);tarifas.saveAndFlush(old);});
        Tarifa created=new Tarifa(tipo,concepto,request.valor().setScale(2,RoundingMode.UNNECESSARY),
                request.moneda()==null?"COP":request.moneda(),now);
        return TarifaResponse.from(tarifas.saveAndFlush(created));
    }
}
