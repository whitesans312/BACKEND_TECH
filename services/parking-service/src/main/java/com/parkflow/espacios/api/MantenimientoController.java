package com.parkflow.espacios.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.parkflow.espacios.application.MantenimientoService;

import jakarta.validation.Valid;

/**
 * GET lo pueden usar ADMINISTRADOR y OPERADOR; POST solo ADMINISTRADOR
 * (reglas de /api/v1/espacios/** en ParkflowSecurityConfiguration).
 */
@RestController
@RequestMapping("/api/v1/espacios/{espacioId}/mantenimientos")
public class MantenimientoController {

    private final MantenimientoService service;

    public MantenimientoController(MantenimientoService service) {
        this.service = service;
    }

    @GetMapping
    public List<MantenimientoResponse> historial(@PathVariable UUID espacioId) {
        return service.historial(espacioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MantenimientoResponse iniciar(@PathVariable UUID espacioId, @AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody MantenimientoRequest request) {
        return service.iniciar(espacioId, UUID.fromString(jwt.getSubject()), request);
    }

    @PostMapping("/finalizar")
    public MantenimientoResponse finalizar(@PathVariable UUID espacioId) {
        return service.finalizar(espacioId);
    }

    @PostMapping("/cancelar")
    public MantenimientoResponse cancelar(@PathVariable UUID espacioId) {
        return service.cancelar(espacioId);
    }
}