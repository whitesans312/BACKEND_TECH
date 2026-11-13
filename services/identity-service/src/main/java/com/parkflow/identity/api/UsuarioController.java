package com.parkflow.identity.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.parkflow.identity.application.IdentityService;

@RestController @RequestMapping("/api/users")
public class UsuarioController {
    private final IdentityService service;
    public UsuarioController(IdentityService service){this.service=service;}
    @PostMapping public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody CrearUsuarioRequest request){var user=service.create(request.usuario(),request.rolIds());return ResponseEntity.created(URI.create("/api/users/"+user.id())).body(user);}
    @GetMapping public List<UsuarioResponse> all(){return service.findAll();}
    @GetMapping("/{id}") public UsuarioResponse one(@PathVariable UUID id){return service.findById(id);}
    @PutMapping("/{id}") public UsuarioResponse update(@PathVariable UUID id,@Valid @RequestBody ActualizarUsuarioRequest request){return service.update(id,request);}
    @PatchMapping("/{id}/status") public UsuarioResponse status(@PathVariable UUID id,@Valid @RequestBody EstadoUsuarioRequest request){return service.setStatus(id,request.estado());}
    @PutMapping("/{id}/roles") public UsuarioResponse roles(@PathVariable UUID id,@Valid @RequestBody RolesUsuarioRequest request){return service.setRoles(id,request.rolIds());}
}
