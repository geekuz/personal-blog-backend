package com.personalblog;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

/** Full-text search over title, summary, tags, and Markdown body of published posts. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {
    "classpath:reset-blog-data.sql",
    "classpath:db/migration/V2__seed_public_posts.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PostSearchIntegrationTest {
    private static final String REACT_TAG = "10000000-0000-0000-0000-000000000001";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void addPosts() {
        insertPost("40000000-0000-0000-0000-000000000001", "react-hooks-guide", "React hooks guide",
            "An overview", "Intro text without the keyword.", "PUBLISHED", "2026-03-01T00:00:00Z", REACT_TAG);
        insertPost("40000000-0000-0000-0000-000000000002", "state-notes", "State notes",
            "Assorted thoughts",
            "## Background\n\nSome words before. Writing custom hooks keeps components small. "
                + "The `useEffect` cleanup runs before the next effect.",
            "PUBLISHED", "2026-09-01T00:00:00Z");
        insertPost("40000000-0000-0000-0000-000000000003", "spring-notes", "Spring notes",
            "Transactions", "Put `@Transactional` on service methods, not controllers.",
            "PUBLISHED", "2026-05-01T00:00:00Z");
        insertPost("40000000-0000-0000-0000-000000000004", "coverage", "Coverage report",
            "Numbers", "Coverage reached 100% today.", "PUBLISHED", "2026-04-01T00:00:00Z");
        insertPost("40000000-0000-0000-0000-000000000005", "secret-draft", "Secret draft",
            "Hidden", "zebraword appears only in this draft.", "DRAFT", null);
    }

    @Test void findsTermsThatOnlyAppearInTheBodyWithASnippet() throws Exception {
        mvc.perform(get("/api/v1/posts").param("q", "@Transactional"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[*].slug", contains("spring-notes")))
            .andExpect(jsonPath("$.items[0].snippet", containsString("@Transactional on service methods")))
            .andExpect(jsonPath("$.items[0].content").doesNotExist());
    }

    @Test void ranksTitleMatchesAboveNewerBodyMatches() throws Exception {
        mvc.perform(get("/api/v1/posts").param("q", "hooks"))
            .andExpect(jsonPath("$.items[*].slug", contains("react-hooks-guide", "state-notes")))
            .andExpect(jsonPath("$.items[0].snippet").doesNotExist())
            .andExpect(jsonPath("$.items[1].snippet", containsString("custom hooks keeps components small")))
            .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test void requiresEveryTermAndIgnoresCase() throws Exception {
        mvc.perform(get("/api/v1/posts").param("q", "USEEFFECT cleanup"))
            .andExpect(jsonPath("$.items[*].slug", contains("state-notes")));
        mvc.perform(get("/api/v1/posts").param("q", "useEffect transactional"))
            .andExpect(jsonPath("$.items", empty()));
    }

    @Test void quotedPhrasesMatchAsAWhole() throws Exception {
        mvc.perform(get("/api/v1/posts").param("q", "\"custom hooks\""))
            .andExpect(jsonPath("$.items[*].slug", contains("state-notes")));
        mvc.perform(get("/api/v1/posts").param("q", "\"hooks custom\""))
            .andExpect(jsonPath("$.items", empty()));
    }

    @Test void treatsSqlWildcardsLiterally() throws Exception {
        mvc.perform(get("/api/v1/posts").param("q", "100%"))
            .andExpect(jsonPath("$.items[*].slug", contains("coverage")));
        mvc.perform(get("/api/v1/posts").param("q", "%"))
            .andExpect(jsonPath("$.items[*].slug", contains("coverage")));
        mvc.perform(get("/api/v1/posts").param("q", "_"))
            .andExpect(jsonPath("$.items", empty()));
    }

    @Test void neverReturnsDrafts() throws Exception {
        mvc.perform(get("/api/v1/posts").param("q", "zebraword"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", empty()))
            .andExpect(content().string(not(containsString("secret-draft"))));
    }

    @Test void combinesSearchWithTagFilter() throws Exception {
        mvc.perform(get("/api/v1/posts").param("q", "hooks").param("tag", "react"))
            .andExpect(jsonPath("$.items[*].slug", contains("react-hooks-guide")));
    }

    @Test void paginatesRankedResults() throws Exception {
        mvc.perform(get("/api/v1/posts").param("q", "hooks").param("size", "1").param("page", "1"))
            .andExpect(jsonPath("$.items[*].slug", contains("state-notes")))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test void listingWithoutQueryIsNewestFirstWithoutSnippets() throws Exception {
        mvc.perform(get("/api/v1/posts").param("size", "2"))
            .andExpect(jsonPath("$.items[*].slug", contains("state-notes", "building-a-blog-api")))
            .andExpect(jsonPath("$.items[0].snippet").doesNotExist());
    }

    @Test void rejectsOverlongQueries() throws Exception {
        mvc.perform(get("/api/v1/posts").param("q", "x".repeat(201)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private void insertPost(String id, String slug, String title, String summary, String content, String status,
                            String publishedAt, String... tagIds) {
        jdbc.update("""
            insert into posts (id, slug, title, summary, content, status, published_at, created_at, updated_at)
            values (?, ?, ?, ?, ?, ?, cast(? as timestamp with time zone), now(), now())
            """, UUID.fromString(id), slug, title, summary, content, status, publishedAt);
        for (String tagId : tagIds) {
            jdbc.update("insert into post_tags (post_id, tag_id) values (?, ?)",
                UUID.fromString(id), UUID.fromString(tagId));
        }
    }
}
