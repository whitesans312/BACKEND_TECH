package com.parkflow.billing.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record AbrirSesionCajaRequest(@NotNull UUID cajaId,@NotNull @DecimalMin("0.00") BigDecimal saldoInicial,
                                     @Size(max=2000) String observacion) { }
