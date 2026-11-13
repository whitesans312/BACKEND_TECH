package com.parkflow.billing.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkflow.billing.application.TarifaService;
import com.parkflow.billing.domain.TipoVehiculo;
import com.parkflow.billing.domain.ConceptoTarifa;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(TarifaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(BillingExceptionHandler.class)
class TarifaControllerMvcTest {
    @Autowired MockMvc mvc;
    @MockitoBean TarifaService service;

    @Test void listsTariffHistory() throws Exception {
        when(service.listar(TipoVehiculo.CARRO)).thenReturn(List.of(new TarifaResponse(
                UUID.randomUUID(), TipoVehiculo.CARRO, ConceptoTarifa.HORA, new BigDecimal("6000.00"),
                "COP", Instant.parse("2026-11-01T00:00:00Z"), null, true, Instant.parse("2026-11-01T00:00:00Z"))));
        mvc.perform(get("/api/billing/tarifas").param("tipoVehiculo", "CARRO"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        verify(service).listar(TipoVehiculo.CARRO);
    }

    @Test void validatesTariffBeforeCallingService() throws Exception {
        mvc.perform(put("/api/billing/tarifas/vigentes/CARRO/HORA").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valor":-1.00,"moneda":"COP"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test void invalidVehicleTypeIsBadRequest() throws Exception {
        mvc.perform(get("/api/billing/tarifas").param("tipoVehiculo", "CAMION"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }
}
