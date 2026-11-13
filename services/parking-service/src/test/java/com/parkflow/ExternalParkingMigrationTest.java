package com.parkflow;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class ExternalParkingMigrationTest {
    @Test void appliesAllParkingMigrationsToConfiguredDisposablePostgres() throws Exception {
        String url=System.getenv("PARKFLOW_TEST_JDBC_URL");
        Assumptions.assumeTrue(url!=null&&!url.isBlank(),"Set PARKFLOW_TEST_JDBC_URL for external PostgreSQL verification.");
        Flyway.configure().dataSource(url,System.getenv("PARKFLOW_TEST_DB_USER"),System.getenv("PARKFLOW_TEST_DB_PASSWORD"))
                .locations("classpath:db/migration").load().migrate();
        try(var c=DriverManager.getConnection(url,System.getenv("PARKFLOW_TEST_DB_USER"),System.getenv("PARKFLOW_TEST_DB_PASSWORD"));
            var s=c.createStatement();var r=s.executeQuery("select count(*) from flyway_schema_history where success")){
            assertTrue(r.next());assertTrue(r.getInt(1)>=3);
        }
    }
}
