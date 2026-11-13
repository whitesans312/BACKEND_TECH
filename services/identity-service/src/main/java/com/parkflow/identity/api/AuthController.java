package com.parkflow.identity.api;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.parkflow.identity.application.IdentityService;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final IdentityService service;
    public AuthController(IdentityService service){this.service=service;}
    @PostMapping("/register") public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody RegistroRequest request){var user=service.register(request);return ResponseEntity.created(URI.create("/api/users/"+user.id())).body(user);}
    @PostMapping("/login") public TokenResponse login(@Valid @RequestBody LoginRequest request){return service.login(request);}
    @GetMapping("/me") public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt){return service.currentUser(UUID.fromString(jwt.getSubject()));}
}
