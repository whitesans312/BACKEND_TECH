package com.parkflow.sesiones.api;

import java.math.BigDecimal;
import java.util.UUID;

public record SalidaResponse(SesionResponse sesion, UUID liquidacionId, String estadoLiquidacion,
                             BigDecimal total, boolean salidaConfirmada) { }
