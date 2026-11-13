package com.parkflow.pisos.api;

import jakarta.validation.constraints.NotNull;

public record PisoStatusRequest(@NotNull Boolean habilitado) {
}
