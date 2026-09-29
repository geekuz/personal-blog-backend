package com.personalblog;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class FlywayUpgradeIntegrationTest {
    @Test
    void upgradesAnExistingVersionNineDatabaseThroughTheLatestSchema() throws Exception {
        String url = "jdbc:h2:mem:flyway-upgrade;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("9").load().migrate();
        Flyway.configure().dataSource(url, "sa", "").target("13").load().migrate();
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            try (var user = connection.prepareStatement("""
                    insert into users (id, email, password_hash, display_name, enabled, email_verified,
                        created_at, updated_at) values (?, ?, ?, ?, true, true, current_timestamp, current_timestamp)
                    """)) {
                user.setObject(1, userId);
                user.setString(2, "existing@example.com");
                user.setString(3, "existing-password-hash");
                user.setString(4, "Existing Reader");
                user.executeUpdate();
            }
            try (var subscription = connection.prepareStatement("""
                    insert into newsletter_subscriptions (id, user_id, subscribed_at)
                    values (?, ?, current_timestamp)
                    """)) {
                subscription.setObject(1, subscriptionId);
                subscription.setObject(2, userId);
                subscription.executeUpdate();
            }
        }
        Flyway.configure().dataSource(url, "sa", "").load().migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            try (var result = statement.executeQuery(
                    "select count(*) from information_schema.columns where table_name = 'posts' and column_name = 'scheduled_at'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isOne();
            }
            try (var result = statement.executeQuery(
                    "select count(*) from information_schema.tables where table_name = 'media_assets'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isOne();
            }
            try (var result = statement.executeQuery("""
                    select email, display_name, confirmed_at from newsletter_subscriptions
                    where id = '""" + subscriptionId + "'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("email")).isEqualTo("existing@example.com");
                assertThat(result.getString("display_name")).isEqualTo("Existing Reader");
                assertThat(result.getObject("confirmed_at")).isNotNull();
            }
        }
    }
}
