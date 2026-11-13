package com.parkflow.espacios.api;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parkflow.espacios.application.EspacioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/espacios")
@Validated
public class EspacioController {

    private final EspacioService espacioService;

    public EspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    @PostMapping
    public ResponseEntity<EspacioResponse> create(@Valid @RequestBody EspacioRequest request) {
        EspacioResponse created = espacioService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/espacios/" + created.id())).body(created);
    }

    @GetMapping
    public List<EspacioResponse> findAll(@RequestParam(required = false) UUID pisoId) {
        return espacioService.findAll(pisoId);
    }

    @GetMapping("/{id}")
    public EspacioResponse findById(@PathVariable UUID id) {
        return espacioService.findById(id);
    }

    @PutMapping("/{id}")
    public EspacioResponse update(@PathVariable UUID id, @Valid @RequestBody EspacioRequest request) {
        return espacioService.update(id, request);
    }

    @PatchMapping("/{id}/habilitado")
    public EspacioResponse setEnabled(@PathVariable UUID id, @Valid @RequestBody EspacioStatusRequest request) {
        return espacioService.setEnabled(id, request.habilitado());
    }
}
