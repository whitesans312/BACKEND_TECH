package com.parkflow.configuracion.api;

import com.parkflow.pisos.api.PisoResponse;
import com.parkflow.zonas.api.ZonaResponse;
import java.util.List;

public record ParqueaderoSetupResponse(PisoResponse piso, List<ZonaResponse> zonas, int espaciosCreados) {}
