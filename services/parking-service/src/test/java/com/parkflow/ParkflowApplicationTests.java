package com.parkflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

import com.parkflow.controller.HealthController;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
class ParkflowApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void appClassLoads() {
        assertThat(ParkflowApplication.class).isNotNull();
    }

    @Test
    void healthEndpointReportsApplicationStatus() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("parking-service"));
    }

    @Test
    void parkingFlywayMigrationsArePackagedWithoutDevelopmentSeeds() throws IOException {
        assertThat(new ClassPathResource("db/migration/V1__parking_schema.sql").exists()).isTrue();
        assertThat(new ClassPathResource("db/migration/V2__parking_invariants.sql").exists()).isTrue();
        assertThat(new ClassPathResource("db/migration/V3__parking_permissions.sql").exists()).isTrue();
        assertThat(new ClassPathResource("db/dev/V900__datos_prueba.sql").exists()).isFalse();
    }
}
