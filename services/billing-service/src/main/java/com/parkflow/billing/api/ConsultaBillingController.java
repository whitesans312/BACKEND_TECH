package com.parkflow.billing.api;

import com.parkflow.billing.application.ConsultaPagoService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class ConsultaBillingController {
    private final ConsultaPagoService service;
    public ConsultaBillingController(ConsultaPagoService service) { this.service = service; }

    @GetMapping("/pagos/{id}")
    public PagoResponse pago(@PathVariable UUID id) { return service.porId(id); }

    @GetMapping("/comprobantes/{id}")
    public ComprobanteResponse comprobante(@PathVariable UUID id) { return service.comprobante(id); }
}
