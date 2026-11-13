package com.parkflow.billing.api;

import com.parkflow.billing.application.CajaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController @RequestMapping("/api/billing/cajas")
public class CajaController {
    private final CajaService service;
    public CajaController(CajaService service){this.service=service;}
    @GetMapping public List<CajaResponse> listar(){return service.listar();}
    @PostMapping public ResponseEntity<CajaResponse> crear(@Valid @RequestBody CrearCajaRequest request){
        CajaResponse created=service.crear(request);return ResponseEntity.created(URI.create("/api/billing/cajas/"+created.id())).body(created);
    }
    @PostMapping("/sesiones") public ResponseEntity<SesionCajaResponse> abrir(@RequestHeader("Idempotency-Key") UUID key,
                                                                                 @AuthenticationPrincipal Jwt jwt,
                                                                                 @Valid @RequestBody AbrirSesionCajaRequest request){
        SesionCajaResponse session=service.abrir(key,request,UUID.fromString(jwt.getSubject()));
        return ResponseEntity.created(URI.create("/api/billing/cajas/sesiones/"+session.id())).body(session);
    }
    @GetMapping("/sesiones/{id}") public SesionCajaResponse porId(@PathVariable UUID id){return service.porId(id);}
    @PostMapping("/sesiones/{id}/cerrar") public SesionCajaResponse cerrar(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody CerrarSesionCajaRequest request){return service.cerrar(id,request,UUID.fromString(jwt.getSubject()));}
}
