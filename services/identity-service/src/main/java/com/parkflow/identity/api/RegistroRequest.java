package com.parkflow.identity.api;

import java.util.Locale;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

public record RegistroRequest(@NotBlank @Size(max=120) String nombre,
                              @NotBlank @Email @Size(max=150) String email,
                              @NotBlank @Size(min=12,max=72) String password) {
    public RegistroRequest { if(email!=null) email=email.strip().toLowerCase(Locale.ROOT); }
    @AssertTrue(message="la contraseña excede el límite compatible con BCrypt")
    public boolean isPasswordWithinBcryptLimit(){return password==null||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72;}
}
