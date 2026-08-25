package com.personalblog;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class FlywayUpgradeIntegrationTest {
    @Test
    void upgradesAnExistingVersionNineDatabaseToScheduledPublishingSchema() throws Exception {
        String url = "jdbc:h2:mem:flyway-upgrade;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("9").load().migrate();
        Flyway.configure().dataSource(url, "sa", "").load().migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery(
                 "select count(*) from information_schema.columns where table_name = 'posts' and column_name = 'scheduled_at'")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isOne();
        }
    }
}
