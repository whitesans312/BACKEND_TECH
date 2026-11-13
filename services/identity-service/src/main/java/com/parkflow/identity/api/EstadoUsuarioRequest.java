package com.parkflow.identity.api;
import com.parkflow.identity.domain.EstadoUsuario;
import jakarta.validation.constraints.NotNull;
public record EstadoUsuarioRequest(@NotNull EstadoUsuario estado) {}
