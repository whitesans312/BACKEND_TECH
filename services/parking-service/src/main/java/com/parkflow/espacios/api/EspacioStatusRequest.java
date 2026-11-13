package com.parkflow.espacios.api;

import jakarta.validation.constraints.NotNull;

public record EspacioStatusRequest(@NotNull Boolean habilitado) {
}
