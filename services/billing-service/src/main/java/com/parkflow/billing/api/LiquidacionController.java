package com.parkflow.billing.api;

import com.parkflow.billing.application.LiquidacionResult;
import com.parkflow.billing.application.LiquidacionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
@Validated
public class LiquidacionController {
    private final LiquidacionService liquidaciones;

    public LiquidacionController(LiquidacionService liquidaciones) { this.liquidaciones = liquidaciones; }

    @PostMapping("/liquidaciones")
    public ResponseEntity<LiquidacionResponse> crear(@AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key") UUID key,
                                               @Valid @RequestBody CrearLiquidacionRequest request) {
        LiquidacionResult result = liquidaciones.crear(key, request,UUID.fromString(jwt.getSubject()));
        if (!result.created()) return ResponseEntity.ok(result.liquidacion());
        return ResponseEntity.created(URI.create("/internal/v1/liquidaciones/" + result.liquidacion().id())).body(result.liquidacion());
    }

    @GetMapping("/liquidaciones/by-idempotency-key/{key}")
    public LiquidacionResponse porClave(@PathVariable UUID key) { return liquidaciones.porClave(key); }

    @GetMapping("/liquidaciones/{id}")
    public LiquidacionResponse porId(@PathVariable UUID id) { return liquidaciones.porId(id); }

    @PostMapping("/liquidaciones/{id}/cancelar")
    public LiquidacionResponse cancelar(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody CancelarLiquidacionRequest request) {
        return liquidaciones.cancelar(id,request,UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/liquidaciones")
    public List<LiquidacionResponse> porSesion(@RequestParam UUID sesionId) { return liquidaciones.porSesion(sesionId); }
}
