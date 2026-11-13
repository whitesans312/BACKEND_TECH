package com.parkflow.billing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkflow.billing.api.CrearLiquidacionRequest;
import com.parkflow.billing.domain.TipoVehiculo;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HashSolicitudTest {
    @Test void matchesSqlV900Sha256Vector() {
        var request = new CrearLiquidacionRequest(
                UUID.fromString("e1000000-0000-0000-0000-000000000001"),
                "PF-2026-000001", TipoVehiculo.CARRO, "ABC123",
                Instant.parse("2026-11-02T13:00:00Z"), Instant.parse("2026-11-02T15:10:00Z"));
        assertThat(new HashSolicitud().calcular(request))
                .isEqualTo("7eb1ebde8ec392b757f03f9dc1da7bafd0a314fc2df0a600f53591df153d0816");
    }

    @Test void cutoffIsExcludedFromHash() {
        var first = new CrearLiquidacionRequest(UUID.randomUUID(), "PF-2026-000001", TipoVehiculo.CARRO,
                "ABC123", Instant.parse("2026-11-02T13:00:00Z"), Instant.parse("2026-11-02T15:10:00Z"));
        var retry = new CrearLiquidacionRequest(first.sesionId(), first.numeroTicket(), first.tipoVehiculo(),
                first.placa(), first.fechaEntrada(), first.fechaCorte().plusSeconds(300));
        assertThat(new HashSolicitud().calcular(first)).isEqualTo(new HashSolicitud().calcular(retry));
    }
}
