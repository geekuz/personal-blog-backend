package com.personalblog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlMigrationIntegrationTest {
    @Container @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired JdbcTemplate jdbc;

    @Test void flywayBuildsPostgresFromZeroWithoutDemoPosts() {
        Integer count = jdbc.queryForObject("select count(*) from posts where status = 'PUBLISHED'", Integer.class);
        assertThat(count).isZero();
        Integer resetTable = jdbc.queryForObject("select count(*) from password_reset_tokens", Integer.class);
        assertThat(resetTable).isZero();
        Integer subscriptions = jdbc.queryForObject("select count(*) from newsletter_subscriptions", Integer.class);
        assertThat(subscriptions).isZero();
        Integer comments = jdbc.queryForObject("select count(*) from post_comments", Integer.class);
        assertThat(comments).isZero();
        Integer deliveries = jdbc.queryForObject("select count(*) from newsletter_deliveries", Integer.class);
        assertThat(deliveries).isZero();
        Integer coverColumns = jdbc.queryForObject("""
            select count(*) from information_schema.columns
            where table_name = 'posts' and column_name in ('cover_image_url', 'cover_image_alt')
            """, Integer.class);
        assertThat(coverColumns).isEqualTo(2);
    }
}
