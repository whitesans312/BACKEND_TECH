package com.parkflow.identity.api;

import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CrearUsuarioRequest(@NotEmpty Set<UUID> rolIds, @NotNull @Valid RegistroRequest usuario) {}
