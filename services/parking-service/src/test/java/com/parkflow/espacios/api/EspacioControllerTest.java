package com.parkflow.espacios.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.parkflow.espacios.application.EspacioService;
import com.parkflow.espacios.domain.TipoEspacio;
import com.parkflow.shared.error.ApiExceptionHandler;
import com.parkflow.shared.error.ConflictException;
import com.parkflow.shared.error.ResourceNotFoundException;

@WebMvcTest(EspacioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class EspacioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EspacioService espacioService;

    @Test
    void createsSpace() throws Exception {
        UUID id = UUID.randomUUID();
        UUID pisoId = UUID.randomUUID();
        when(espacioService.create(any(EspacioRequest.class))).thenReturn(
                new EspacioResponse(id, pisoId, "P1", "A01", TipoEspacio.CARRO,
                        true, false, true, null, null));

        mockMvc.perform(post("/api/v1/espacios")
                        .contentType("application/json")
                        .content("{\"zonaId\":\"" + pisoId + "\",\"codigo\":\"A01\",\"tipo\":\"CARRO\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/espacios/" + id))
                .andExpect(jsonPath("$.disponible").value(true));
    }

    @Test
    void rejectsMissingFloorOrUnsupportedType() throws Exception {
        mockMvc.perform(post("/api/v1/espacios")
                        .contentType("application/json")
                        .content("{\"codigo\":\"A01\",\"tipo\":\"BICICLETA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsConflictWhenSpaceHasActiveSessionAndCannotBeDisabled() throws Exception {
        UUID id = UUID.randomUUID();
        when(espacioService.setEnabled(id, false))
                .thenThrow(new ConflictException("No se puede deshabilitar el espacio: tiene una sesión activa."));

        mockMvc.perform(patch("/api/v1/espacios/{id}/habilitado", id)
                        .contentType("application/json")
                        .content("{\"habilitado\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void listsSpacesIncludingDerivedAvailabilityAndCanFilterByFloor() throws Exception {
        UUID pisoId = UUID.randomUUID();
        when(espacioService.findAll(pisoId)).thenReturn(List.of(
                new EspacioResponse(UUID.randomUUID(), pisoId, "P1", "A01", TipoEspacio.CARRO,
                        true, true, false, null, null)));

        mockMvc.perform(get("/api/v1/espacios").queryParam("pisoId", pisoId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ocupado").value(true))
                .andExpect(jsonPath("$[0].disponible").value(false));
        verify(espacioService).findAll(eq(pisoId));
    }

    @Test
    void supportsLookupAndUpdateById() throws Exception {
        UUID id = UUID.randomUUID();
        UUID pisoId = UUID.randomUUID();
        when(espacioService.findById(id)).thenReturn(new EspacioResponse(id, pisoId, "P1", "A02",
                TipoEspacio.MOTO, true, false, true, null, null));
        when(espacioService.update(eq(id), any(EspacioRequest.class))).thenReturn(
                new EspacioResponse(id, pisoId, "P1", "A02", TipoEspacio.MOTO, true, false, true, null, null));

        mockMvc.perform(get("/api/v1/espacios/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("MOTO"));

        mockMvc.perform(put("/api/v1/espacios/{id}", id)
                        .contentType("application/json")
                        .content("{\"zonaId\":\"" + pisoId + "\",\"codigo\":\"A02\",\"tipo\":\"MOTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("A02"));
    }

    @Test
    void returnsNotFoundForUnknownSpace() throws Exception {
        UUID id = UUID.randomUUID();
        when(espacioService.findById(id)).thenThrow(new ResourceNotFoundException("No existe el espacio solicitado."));

        mockMvc.perform(get("/api/v1/espacios/{id}", id)).andExpect(status().isNotFound());
    }
}
