package com.ergpos.app.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ergpos.app.businessconfig.api.ConfiguracionNegocioController;
import com.ergpos.app.businessconfig.application.ConfiguracionNegocioService;
import com.ergpos.app.businessconfig.domain.ConfiguracionNegocio;
import com.ergpos.app.services.AuditoriaService;

@ExtendWith(MockitoExtension.class)
class ConfiguracionNegocioControllerTest {

    @Mock
    private ConfiguracionNegocioService service;

    @Mock
    private AuditoriaService auditoriaService;

    private MockMvc mvc;
    private ConfiguracionNegocio config;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                new ConfiguracionNegocioController(service, auditoriaService)).build();
        config = new ConfiguracionNegocio();
        config.setClave("TAX_PERCENTAGE");
        config.setValor("19");
        config.setCategoria("FISCAL");
        config.setDescripcion("Porcentaje de impuesto");
    }

    @Test
    void readRoutesKeepTheirPathsAndResponseShapes() throws Exception {
        when(service.findAll()).thenReturn(List.of(config));
        when(service.findAllGroupedByCategoria()).thenReturn(Map.of("FISCAL", List.of(config)));
        when(service.findByCategoria("FISCAL")).thenReturn(List.of(config));
        when(service.findByClave("TAX_PERCENTAGE")).thenReturn(Optional.of(config));
        when(service.getPlantilla("RETAIL")).thenReturn(List.of(config));

        mvc.perform(get("/api/configuracion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clave").value("TAX_PERCENTAGE"))
                .andExpect(jsonPath("$[0].valor").value("19"));
        mvc.perform(get("/api/configuracion/agrupada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.FISCAL[0].clave").value("TAX_PERCENTAGE"));
        mvc.perform(get("/api/configuracion/categoria/fiscal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria").value("FISCAL"));
        mvc.perform(get("/api/configuracion/TAX_PERCENTAGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clave").value("TAX_PERCENTAGE"));
        mvc.perform(get("/api/configuracion/NO_EXISTE"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/configuracion/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clave").value("TAX_PERCENTAGE"));
        mvc.perform(get("/api/configuracion/plantillas/RETAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valor").value("19"));
    }

    @Test
    void writeRoutesKeepStatusBodyAndAuditBehavior() throws Exception {
        when(service.save(any(ConfiguracionNegocio.class))).thenReturn(config);
        when(service.saveAll(anyList())).thenReturn(List.of(config));
        when(service.aplicarPlantilla("RETAIL")).thenReturn(List.of(config));

        String item = """
                {"clave":"TAX_PERCENTAGE","valor":"19","categoria":"FISCAL","descripcion":"Porcentaje de impuesto"}
                """;

        mvc.perform(post("/api/configuracion").contentType("application/json").content(item))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clave").value("TAX_PERCENTAGE"));
        mvc.perform(put("/api/configuracion/TAX_PERCENTAGE").contentType("application/json").content(item))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value("19"));
        mvc.perform(put("/api/configuracion").contentType("application/json").content("[" + item + "]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clave").value("TAX_PERCENTAGE"));
        mvc.perform(post("/api/configuracion/import").contentType("application/json").content("[" + item + "]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clave").value("TAX_PERCENTAGE"));
        mvc.perform(post("/api/configuracion/plantillas/RETAIL/aplicar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clave").value("TAX_PERCENTAGE"));
        mvc.perform(delete("/api/configuracion/TAX_PERCENTAGE"))
                .andExpect(status().isNoContent());

        verify(auditoriaService).registrarActual(
                "CREAR_CONFIGURACION", "CONFIGURACION", null,
                "Se creo parametro de configuracion: TAX_PERCENTAGE");
        verify(auditoriaService).registrarActual(
                "EDITAR_CONFIGURACION", "CONFIGURACION", null,
                "Se actualizo parametro de configuracion: TAX_PERCENTAGE");
        verify(auditoriaService).registrarActual(
                "ELIMINAR_CONFIGURACION", "CONFIGURACION", null,
                "Se elimino parametro de configuracion: TAX_PERCENTAGE");
        verify(auditoriaService).registrarActual(
                "IMPORTAR_CONFIGURACION", "CONFIGURACION", null,
                "Se importaron 1 parametros de configuracion.");
        verify(auditoriaService).registrarActual(
                "APLICAR_PLANTILLA_CONFIGURACION", "CONFIGURACION", null,
                "Se aplico plantilla de configuracion: RETAIL");
    }
}
