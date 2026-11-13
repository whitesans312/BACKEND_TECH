package com.parkflow.sesiones.api;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.parkflow.sesiones.application.SesionService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController @RequestMapping("/api/sesiones")
public class SesionController {
    private final SesionService service;
    public SesionController(SesionService service){this.service=service;}
    @PostMapping("/entradas") public ResponseEntity<SesionResponse> entrada(@RequestHeader("Idempotency-Key") UUID operationId,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody EntradaRequest r){var s=service.registrarEntrada(r,UUID.fromString(jwt.getSubject()),operationId);return ResponseEntity.created(URI.create("/api/sesiones/"+s.numeroTicket())).body(s);}
    @GetMapping public List<SesionResponse> listar(@RequestParam(defaultValue="false") boolean activas){return service.findAll(activas);}
    @GetMapping("/{ticket}") public SesionResponse ticket(@PathVariable String ticket){return service.findByTicket(ticket);}
    @PostMapping("/{ticket}/salida") public ResponseEntity<SalidaResponse> salida(@PathVariable String ticket,
            @RequestHeader("Idempotency-Key") UUID operationId,@AuthenticationPrincipal Jwt jwt){
        var result=service.solicitarSalida(ticket,operationId,jwt);
        return ResponseEntity.status(result.salidaConfirmada()?200:202).body(result);
    }
    @GetMapping("/buscar-placa") public List<SesionResponse> buscarPlaca(@RequestParam String placa){return service.buscarPorPlaca(placa);}
    @PutMapping("/{ticket}/placa") public SesionResponse corregirPlaca(@PathVariable String ticket,
            @RequestHeader("Idempotency-Key") UUID operationId,@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CorreccionPlacaRequest request){
        return service.corregirPlaca(ticket,operationId,UUID.fromString(jwt.getSubject()),request);
    }
}
