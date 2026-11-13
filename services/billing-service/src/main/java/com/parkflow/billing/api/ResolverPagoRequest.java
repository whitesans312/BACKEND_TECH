package com.parkflow.billing.api;

import com.parkflow.billing.domain.MetodoPago;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ResolverPagoRequest(@NotNull MetodoPago metodo,@NotNull UUID sesionCajaId) { }
