package com.parkflow.billing.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ActualizarTarifaRequest(@NotNull @Digits(integer=10,fraction=2) @PositiveOrZero BigDecimal valor,
                                      @Pattern(regexp="[A-Z]{3}") String moneda) {}
