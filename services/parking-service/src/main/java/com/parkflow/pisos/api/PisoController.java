package com.parkflow.pisos.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parkflow.pisos.application.PisoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/pisos")
@Validated
public class PisoController {

    private final PisoService pisoService;

    public PisoController(PisoService pisoService) {
        this.pisoService = pisoService;
    }

    @PostMapping
    public ResponseEntity<PisoResponse> create(@Valid @RequestBody PisoRequest request) {
        PisoResponse created = pisoService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/pisos/" + created.id())).body(created);
    }

    @GetMapping
    public List<PisoResponse> findAll() {
        return pisoService.findAll();
    }

    @GetMapping("/{id}")
    public PisoResponse findById(@PathVariable UUID id) {
        return pisoService.findById(id);
    }

    @PutMapping("/{id}")
    public PisoResponse update(@PathVariable UUID id, @Valid @RequestBody PisoRequest request) {
        return pisoService.update(id, request);
    }

    @PatchMapping("/{id}/habilitado")
    public PisoResponse setEnabled(@PathVariable UUID id, @Valid @RequestBody PisoStatusRequest request) {
        return pisoService.setEnabled(id, request.habilitado());
    }
}
