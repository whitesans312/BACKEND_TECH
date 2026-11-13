package com.parkflow.identity.api;

import java.util.Locale;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioRequest(@NotBlank @Size(max=120) String nombre,
                                       @NotBlank @Email @Size(max=120) String email) {
    public ActualizarUsuarioRequest { if(email!=null) email=email.strip().toLowerCase(Locale.ROOT); }
}
