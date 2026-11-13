package com.parkflow.billing.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazarPagoRequest(@NotBlank @Size(max=200) String motivo) { }
