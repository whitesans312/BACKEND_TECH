package com.parkflow.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.DriverManager;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class IdentityMigrationPostgresTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.15")
            .withDatabaseName("identity_test").withUsername("test").withPassword("test")
            .withInitScript("test_roles.sql");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void seedsRolesAndKeepsAuthorizationPurposesSeparate() throws Exception {
        try (var c = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var s = c.createStatement()) {
            try (var r = s.executeQuery("SELECT count(*) FROM roles")) { r.next(); assertEquals(2, r.getInt(1)); }
            try (var r = s.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name IN ('autorizaciones_excepcionales','autorizaciones_cargo_abandono')")) {
                r.next(); assertEquals(2, r.getInt(1));
            }
        }
    }
}
