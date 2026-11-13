package com.parkflow.pisos.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkflow.pisos.application.PisoService;
import com.parkflow.shared.error.ApiExceptionHandler;
import com.parkflow.shared.error.ConflictException;
import com.parkflow.shared.error.ResourceNotFoundException;

@WebMvcTest(PisoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class PisoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PisoService pisoService;

    @Test
    void createsFloorAndReturnsLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(pisoService.create(any(PisoRequest.class))).thenReturn(
                new PisoResponse(id, "P1", "Piso uno", (short) 1, true, null, null));

        mockMvc.perform(post("/api/v1/pisos")
                        .contentType("application/json")
                        .content("{\"codigo\":\"P1\",\"nombre\":\"Piso uno\",\"orden\":1}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/pisos/" + id))
                .andExpect(jsonPath("$.codigo").value("P1"))
                .andExpect(jsonPath("$.habilitado").value(true));
    }

    @Test
    void rejectsInvalidFloorFields() throws Exception {
        mockMvc.perform(post("/api/v1/pisos")
                        .contentType("application/json")
                        .content("{\"codigo\":\"  \",\"nombre\":\"\",\"orden\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La solicitud contiene campos inválidos."))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void returnsProblemDetailForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/pisos")
                        .contentType("application/json")
                        .content("{malformed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.detail").value(
                        "El cuerpo JSON está mal formado o contiene valores no admitidos."));
    }

    @Test
    void returnsConflictForDuplicateFloorCodeOrOrder() throws Exception {
        when(pisoService.create(any(PisoRequest.class)))
                .thenThrow(new ConflictException("Ya existe un piso con ese código."));

        mockMvc.perform(post("/api/v1/pisos")
                        .contentType("application/json")
                        .content("{\"codigo\":\"P1\",\"nombre\":\"Piso uno\",\"orden\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un piso con ese código."));
    }

    @Test
    void returnsNotFoundForUnknownFloor() throws Exception {
        UUID id = UUID.randomUUID();
        when(pisoService.findById(id)).thenThrow(new ResourceNotFoundException("No existe el piso solicitado."));

        mockMvc.perform(get("/api/v1/pisos/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsBadRequestForMalformedUuid() throws Exception {
        mockMvc.perform(get("/api/v1/pisos/no-es-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parámetro inválido"));
    }

    @Test
    void returnsMethodNotAllowedWithAllowHeader() throws Exception {
        mockMvc.perform(delete("/api/v1/pisos/{id}", UUID.randomUUID()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.title").value("Método no permitido"));
    }

    @Test
    void hidesUnexpectedExceptionDetails() throws Exception {
        when(pisoService.findAll()).thenThrow(new IllegalStateException("internal SQL details"));

        mockMvc.perform(get("/api/v1/pisos"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ocurrió un error interno al procesar la solicitud."))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("internal SQL details"))));
    }

    @Test
    void updatesFloorAndCanToggleEnabledState() throws Exception {
        UUID id = UUID.randomUUID();
        when(pisoService.update(eq(id), any(PisoRequest.class))).thenReturn(
                new PisoResponse(id, "P1", "Piso actualizado", (short) 2, true, null, null));
        when(pisoService.setEnabled(id, false)).thenReturn(
                new PisoResponse(id, "P1", "Piso actualizado", (short) 2, false, null, null));

        mockMvc.perform(put("/api/v1/pisos/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new PisoRequest("P1", "Piso actualizado", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Piso actualizado"));

        mockMvc.perform(patch("/api/v1/pisos/{id}/habilitado", id)
                        .contentType("application/json")
                        .content("{\"habilitado\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitado").value(false));
        verify(pisoService).setEnabled(id, false);
    }

    @Test
    void listsFloors() throws Exception {
        when(pisoService.findAll()).thenReturn(List.of(
                new PisoResponse(UUID.randomUUID(), "P1", "Piso uno", (short) 1, true, null, null)));

        mockMvc.perform(get("/api/v1/pisos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("P1"));
    }
}
