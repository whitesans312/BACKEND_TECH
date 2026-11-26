package com.parkflow.reservas.api;

import com.parkflow.reservas.application.ReservaService;
import com.parkflow.reservas.domain.EstadoReserva;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservas")
@Validated
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@RequestHeader("Idempotency-Key") UUID operationId,
                                                 @AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody CrearReservaRequest request) {
        ReservaResponse created = service.crear(request, UUID.fromString(jwt.getSubject()), operationId);
        return ResponseEntity.created(URI.create("/api/v1/reservas/" + created.id())).body(created);
    }

    @GetMapping
    public List<ReservaResponse> listar(@RequestParam(required = false) EstadoReserva estado,
                                        @RequestParam(required = false) String placa) {
        return service.listar(estado, placa);
    }

    @GetMapping("/{id}")
    public ReservaResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @PostMapping("/{id}/check-in")
    public ReservaCheckInResponse checkIn(@PathVariable UUID id,
                                          @RequestHeader("Idempotency-Key") UUID operationId,
                                          @AuthenticationPrincipal Jwt jwt) {
        return service.checkIn(id, UUID.fromString(jwt.getSubject()), operationId);
    }

    @PatchMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable UUID id,
                                    @AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody(required = false) CancelarReservaRequest request) {
        return service.cancelar(id, UUID.fromString(jwt.getSubject()), request);
    }
}
