package com.parkflow.billing.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarLiquidacionRequest(@NotBlank @Size(max=1000) String motivo) { }
