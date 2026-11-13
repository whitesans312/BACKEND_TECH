package com.parkflow.billing;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class ExternalBillingMigrationTest {
    @Test void appliesAllBillingMigrationsToConfiguredDisposablePostgres() throws Exception {
        String url=System.getenv("BILLING_TEST_JDBC_URL");
        Assumptions.assumeTrue(url!=null&&!url.isBlank(),"Set BILLING_TEST_JDBC_URL for external PostgreSQL verification.");
        String user=System.getenv("BILLING_TEST_DB_USER"),password=System.getenv("BILLING_TEST_DB_PASSWORD");
        Flyway.configure().dataSource(url,user,password).locations("classpath:db/migration").load().migrate();
        try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement();
            var r=s.executeQuery("select count(*) from flyway_schema_history where success")){
            assertTrue(r.next());assertTrue(r.getInt(1)>=3);
        }
    }
}
