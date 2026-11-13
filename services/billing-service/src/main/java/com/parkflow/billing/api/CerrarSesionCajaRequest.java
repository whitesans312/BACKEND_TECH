package com.parkflow.billing.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CerrarSesionCajaRequest(@NotNull @DecimalMin("0.00") BigDecimal saldoContado,
                                      @Size(max=2000) String observacion) { }
