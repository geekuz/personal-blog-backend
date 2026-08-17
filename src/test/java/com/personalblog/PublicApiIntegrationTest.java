package com.personalblog;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {
    "classpath:reset-blog-data.sql",
    "classpath:db/migration/V2__seed_public_posts.sql",
    "classpath:db/migration/V3__import_original_frontend_posts.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PublicApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void addDraft() {
        jdbc.update("delete from posts where slug = 'secret-draft'");
        jdbc.update("""
            insert into posts (id, slug, title, summary, content, status, published_at, created_at, updated_at)
            values (?, 'secret-draft', 'Secret React Draft', 'React text that must stay private',
                    'draft body', 'DRAFT', null, now(), now())
            """, java.util.UUID.fromString("30000000-0000-0000-0000-000000000001"));
    }

    @Test void listsPublishedPostsNewestFirstWithoutContent() throws Exception {
        mvc.perform(get("/api/v1/posts").param("size", "2"))
            .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(jsonPath("$.items[0].slug").value("hello-world"))
            .andExpect(jsonPath("$.items[0].content").doesNotExist())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test void combinesSearchAndTagFilters() throws Exception {
        mvc.perform(get("/api/v1/posts").param("q", "react").param("tag", "learning"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].slug").value("why-i-chose-react"));
    }

    @Test void draftCannotBeDiscoveredViaListSearchOrDetail() throws Exception {
        mvc.perform(get("/api/v1/posts").param("q", "secret"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items", empty()));
        mvc.perform(get("/api/v1/posts/secret-draft"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test void returnsPostDetailsAndTags() throws Exception {
        mvc.perform(get("/api/v1/posts/why-i-chose-react"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content", startsWith("There are a lot of frontend frameworks.")))
            .andExpect(jsonPath("$.tags", contains("learning", "react")))
            .andExpect(jsonPath("$.readingTimeMinutes").value(1));
        mvc.perform(get("/api/v1/tags"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(6)))
            .andExpect(jsonPath("$.items[0].name").value("CSS"));
    }

    @Test void validatesPaginationWithStandardError() throws Exception {
        mvc.perform(get("/api/v1/posts").param("size", "51"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors.size").value("must be less than or equal to 50"));
        mvc.perform(get("/api/v1/posts").param("page", "not-a-number"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors.page").exists());
    }

    @Test void allowsConfiguredViteOrigin() throws Exception {
        mvc.perform(get("/api/v1/posts").header("Origin", "http://localhost:5173"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
