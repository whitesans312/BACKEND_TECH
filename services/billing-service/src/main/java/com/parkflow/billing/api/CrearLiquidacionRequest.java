package com.parkflow.billing.api;

import com.parkflow.billing.domain.TipoVehiculo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.UUID;

public record CrearLiquidacionRequest(@NotNull UUID sesionId,
                                      @NotNull @Pattern(regexp="PF-[0-9]{4}-[0-9]{6,}") String numeroTicket,
                                      @NotNull TipoVehiculo tipoVehiculo,
                                      @NotNull @Pattern(regexp="[A-Z0-9]{4,10}") String placa,
                                      @NotNull Instant fechaEntrada,
                                      @NotNull Instant fechaCorte) { }
