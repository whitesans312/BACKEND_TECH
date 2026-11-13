package com.parkflow.zonas.api;

import jakarta.validation.constraints.NotNull;

public record ZonaStatusRequest(@NotNull Boolean habilitada) {}
