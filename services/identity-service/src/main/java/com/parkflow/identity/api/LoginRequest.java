package com.parkflow.identity.api;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import java.util.Locale;
public record LoginRequest(@NotBlank @Email @Size(max=120) String email,@NotBlank @Size(max=72) String password) {
    public LoginRequest { if(email!=null) email=email.strip().toLowerCase(Locale.ROOT); }
    @AssertTrue(message="la contraseña excede el límite compatible con BCrypt")
    public boolean isPasswordWithinBcryptLimit(){return password==null||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72;}
}
