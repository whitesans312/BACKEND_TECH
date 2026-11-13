package com.parkflow.billing.api;

import com.parkflow.billing.application.TarifaService;
import com.parkflow.billing.domain.TipoVehiculo;
import com.parkflow.billing.domain.ConceptoTarifa;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/tarifas")
@Validated
public class TarifaController {
    private final TarifaService service;
    public TarifaController(TarifaService service) { this.service = service; }

    @GetMapping
    public List<TarifaResponse> listar(@RequestParam(required = false) TipoVehiculo tipoVehiculo) {
        return service.listar(tipoVehiculo);
    }

    @PutMapping("/vigentes/{tipoVehiculo}/{concepto}")
    public ResponseEntity<TarifaResponse> reemplazar(@PathVariable TipoVehiculo tipoVehiculo, @PathVariable ConceptoTarifa concepto,
                                                      @Valid @RequestBody ActualizarTarifaRequest request) {
        return ResponseEntity.ok(service.reemplazarVigente(tipoVehiculo, concepto, request));
    }
}
