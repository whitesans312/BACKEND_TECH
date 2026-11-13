package com.parkflow.billing.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkflow.billing.application.ConsultaPagoService;
import com.parkflow.billing.application.LiquidacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(LiquidacionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(BillingExceptionHandler.class)
class LiquidacionControllerMvcTest {
    @Autowired MockMvc mvc;
    @MockitoBean LiquidacionService liquidaciones;
    @MockitoBean ConsultaPagoService consultas;

    @Test void missingIdempotencyKeyIsProblem400() throws Exception {
        mvc.perform(post("/internal/v1/liquidaciones").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test void malformedIdempotencyKeyIsProblem400() throws Exception {
        mvc.perform(post("/internal/v1/liquidaciones").header("Idempotency-Key", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }
}
