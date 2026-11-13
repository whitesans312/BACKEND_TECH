package com.parkflow.billing.application;

import com.parkflow.billing.api.LiquidacionResponse;

public record LiquidacionResult(LiquidacionResponse liquidacion, boolean created) { }
