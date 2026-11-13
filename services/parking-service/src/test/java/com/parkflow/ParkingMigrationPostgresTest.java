package com.parkflow;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
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
class ParkingMigrationPostgresTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.15")
            .withDatabaseName("parking_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("test_roles.sql");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    @Test
    void migratesWithoutPermanentVehicleCatalogAndEnforcesActiveUniqueness() throws Exception {
        try (Connection c = connection()) {
            try (var s = c.createStatement(); var r = s.executeQuery("SELECT to_regclass('public.vehiculos') IS NULL")) {
                assertTrue(r.next());
                assertTrue(r.getBoolean(1));
            }
            UUID floor = UUID.randomUUID();
            UUID zone = UUID.randomUUID();
            UUID space1 = UUID.randomUUID();
            UUID space2 = UUID.randomUUID();
            try (var s = c.createStatement()) {
                s.executeUpdate("INSERT INTO pisos(id,numero,codigo,nombre) VALUES ('"+floor+"',1,'P1','Piso 1')");
                s.executeUpdate("INSERT INTO zonas(id,piso_id,codigo,nombre) VALUES ('"+zone+"','"+floor+"','A','Zona A')");
                s.executeUpdate("INSERT INTO espacios(id,zona_id,codigo,tipo_vehiculo) VALUES ('"+space1+"','"+zone+"','A1','CARRO'),('"+space2+"','"+zone+"','A2','CARRO')");
                s.executeUpdate("INSERT INTO sesiones_parqueo(numero_tiquete,espacio_id,placa_ingreso,placa_actual,tipo_vehiculo,operador_entrada_id) VALUES ('T1','"+space1+"','ABC123','ABC123','CARRO','"+UUID.randomUUID()+"')");
            }
            SQLException duplicate = assertThrows(SQLException.class, () -> {
                try (var s = c.createStatement()) {
                    s.executeUpdate("INSERT INTO sesiones_parqueo(numero_tiquete,espacio_id,placa_ingreso,placa_actual,tipo_vehiculo,operador_entrada_id) VALUES ('T2','"+space2+"','ABC123','ABC123','CARRO','"+UUID.randomUUID()+"')");
                }
            });
            assertEquals("23505", duplicate.getSQLState());
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
