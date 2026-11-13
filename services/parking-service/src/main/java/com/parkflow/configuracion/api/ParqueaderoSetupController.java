package com.parkflow.configuracion.api;

import com.parkflow.configuracion.application.ParqueaderoSetupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parqueadero/configuracion")
public class ParqueaderoSetupController {
    private final ParqueaderoSetupService service;
    public ParqueaderoSetupController(ParqueaderoSetupService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParqueaderoSetupResponse create(@Valid @RequestBody ParqueaderoSetupRequest request) {
        return service.create(request);
    }
}
