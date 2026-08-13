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

    @Test void flywayBuildsPostgresFromZeroAndSeedsThreePosts() {
        Integer count = jdbc.queryForObject("select count(*) from posts where status = 'PUBLISHED'", Integer.class);
        assertThat(count).isEqualTo(3);
    }
}
