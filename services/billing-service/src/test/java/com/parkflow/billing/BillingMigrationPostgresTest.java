package com.parkflow.billing;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class BillingMigrationPostgresTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.15")
            .withDatabaseName("billing_test").withUsername("test").withPassword("test")
            .withInitScript("test_roles.sql");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void seedsTariffsAndRejectsFreeSettlementForAbandonmentCharge() throws Exception {
        try (var c = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var s = c.createStatement()) {
            try (var r = s.executeQuery("SELECT count(*) FROM tarifas")) { r.next(); assertEquals(6, r.getInt(1)); }
            UUID charge = UUID.randomUUID();
            UUID caseId = UUID.randomUUID();
            UUID authorization = UUID.randomUUID();
            s.executeUpdate("INSERT INTO cargos_abandono(id,caso_abandono_id,autorizacion_identity_id,importe_autorizado,motivo,administrador_autorizador_id,autorizado_en,estado,clave_idempotencia) VALUES ('"+charge+"','"+caseId+"','"+authorization+"',10000,'Cargo autorizado','"+UUID.randomUUID()+"',now(),'AUTORIZADO','"+UUID.randomUUID()+"')");
            SQLException freeCharge = assertThrows(SQLException.class, () -> s.executeUpdate(
                    "INSERT INTO liquidaciones(cargo_abandono_id,hora_corte,subtotal,recargos,descuentos,total,estado,clave_idempotencia,hash_solicitud,operador_liquida_id) VALUES ('"+charge+"',now(),0,0,0,0,'GRATUITA','"+UUID.randomUUID()+"',repeat('a',64),'"+UUID.randomUUID()+"')"));
            assertEquals("23514", freeCharge.getSQLState());
        }
    }
}
