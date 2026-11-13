package com.parkflow.billing.api;

import com.parkflow.billing.application.ResolucionPagoService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/internal/v1/pagos")
public class ResolucionPagoController {
    private final ResolucionPagoService service;
    public ResolucionPagoController(ResolucionPagoService service) { this.service = service; }

    @PostMapping("/{id}/resolver")
    public PagoResponse aprobar(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ResolverPagoRequest request) {
        return service.aprobar(id, request,UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/{id}/rechazar")
    public PagoResponse rechazar(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody RechazarPagoRequest request) {
        return service.rechazar(id, request,UUID.fromString(jwt.getSubject()));
    }
}
