package com.parkflow.zonas.api;

import com.parkflow.zonas.application.ZonaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/zonas")
public class ZonaController {
    private final ZonaService service;
    public ZonaController(ZonaService service) { this.service=service; }
    @PostMapping public ResponseEntity<ZonaResponse> crear(@Valid @RequestBody ZonaRequest r) {
        ZonaResponse z=service.crear(r); return ResponseEntity.created(URI.create("/api/zonas/"+z.id())).body(z);
    }
    @GetMapping public List<ZonaResponse> listar(@RequestParam(required=false) UUID pisoId) { return service.listar(pisoId); }
    @PutMapping("/{id}") public ZonaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ZonaRequest request) { return service.actualizar(id, request); }
    @PatchMapping("/{id}/habilitada") public ZonaResponse habilitar(@PathVariable UUID id, @Valid @RequestBody ZonaStatusRequest request) { return service.habilitar(id, request.habilitada()); }
}
