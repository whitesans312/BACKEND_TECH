package com.parkflow.billing.application;

import java.math.BigDecimal;
import com.parkflow.billing.domain.Tarifa;

public record CalculoLiquidacion(int minutos, int minutosFacturables, int unidades, BigDecimal total,
                                 String concepto, Tarifa tarifaAplicada) { }
