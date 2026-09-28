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

/**
 * Seed order (newest first): building-a-blog-api (java, spring-boot), learning-java-21 (learning, java),
 * why-i-chose-react (react, learning). Tests add a newer java-deep-dive and a draft that must stay hidden.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {
    "classpath:reset-blog-data.sql",
    "classpath:db/migration/V2__seed_public_posts.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PostNavigationIntegrationTest {
    private static final String JAVA_TAG = "10000000-0000-0000-0000-000000000003";
    private static final String SPRING_TAG = "10000000-0000-0000-0000-000000000004";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void addPosts() {
        insertPost("30000000-0000-0000-0000-000000000001", "java-deep-dive", "PUBLISHED", "2026-09-01T12:00:00Z",
            JAVA_TAG, SPRING_TAG);
        insertPost("30000000-0000-0000-0000-000000000002", "secret-draft", "DRAFT", null, JAVA_TAG, SPRING_TAG);
    }

    @Test void middlePostLinksToOlderAndNewerNeighbours() throws Exception {
        mvc.perform(get("/api/v1/posts/learning-java-21"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.previousPost.slug").value("why-i-chose-react"))
            .andExpect(jsonPath("$.previousPost.title").value("Why I Chose React to Learn First"))
            .andExpect(jsonPath("$.nextPost.slug").value("building-a-blog-api"))
            .andExpect(jsonPath("$.previousPost.content").doesNotExist());
    }

    @Test void oldestAndNewestPostsHaveNoOuterNeighbour() throws Exception {
        mvc.perform(get("/api/v1/posts/why-i-chose-react"))
            .andExpect(jsonPath("$.previousPost").value(nullValue()))
            .andExpect(jsonPath("$.nextPost.slug").value("learning-java-21"));
        mvc.perform(get("/api/v1/posts/java-deep-dive"))
            .andExpect(jsonPath("$.previousPost.slug").value("building-a-blog-api"))
            .andExpect(jsonPath("$.nextPost").value(nullValue()));
    }

    @Test void relatedPostsRankBySharedTagsThenRecencyAndHideDrafts() throws Exception {
        mvc.perform(get("/api/v1/posts/building-a-blog-api"))
            .andExpect(jsonPath("$.relatedPosts[*].slug", contains("java-deep-dive", "learning-java-21")))
            .andExpect(jsonPath("$.relatedPosts[0].title").value("Java-deep-dive"))
            .andExpect(jsonPath("$.relatedPosts[0].summary").value("Summary of java-deep-dive"))
            .andExpect(jsonPath("$.relatedPosts[0].content").doesNotExist())
            .andExpect(content().string(not(containsString("secret-draft"))));
        mvc.perform(get("/api/v1/posts/learning-java-21"))
            .andExpect(jsonPath("$.relatedPosts[*].slug",
                contains("java-deep-dive", "building-a-blog-api", "why-i-chose-react")));
    }

    @Test void relatedPostsAreCappedAtThree() throws Exception {
        for (int i = 3; i <= 6; i++) {
            insertPost("30000000-0000-0000-0000-00000000000" + i, "extra-java-" + i, "PUBLISHED",
                "2026-08-1" + i + "T00:00:00Z", JAVA_TAG);
        }
        mvc.perform(get("/api/v1/posts/building-a-blog-api"))
            .andExpect(jsonPath("$.relatedPosts", hasSize(3)))
            .andExpect(jsonPath("$.relatedPosts[0].slug").value("java-deep-dive"));
    }

    @Test void postWithoutTagsHasNoRelatedPosts() throws Exception {
        insertPost("30000000-0000-0000-0000-000000000009", "untagged", "PUBLISHED", "2026-05-01T00:00:00Z");
        mvc.perform(get("/api/v1/posts/untagged"))
            .andExpect(jsonPath("$.relatedPosts", empty()))
            .andExpect(jsonPath("$.nextPost.slug").value("why-i-chose-react"));
    }

    @Test void neighboursWithIdenticalPublishDatesFollowListOrdering() throws Exception {
        insertPost("30000000-0000-0000-0000-00000000000a", "same-time-a", "PUBLISHED", "2026-09-10T00:00:00Z");
        insertPost("30000000-0000-0000-0000-00000000000b", "same-time-b", "PUBLISHED", "2026-09-10T00:00:00Z");
        // The public list sorts publishedAt desc, then id desc: same-time-b, same-time-a, java-deep-dive, ...
        mvc.perform(get("/api/v1/posts").param("size", "3"))
            .andExpect(jsonPath("$.items[*].slug", contains("same-time-b", "same-time-a", "java-deep-dive")));
        mvc.perform(get("/api/v1/posts/same-time-a"))
            .andExpect(jsonPath("$.nextPost.slug").value("same-time-b"))
            .andExpect(jsonPath("$.previousPost.slug").value("java-deep-dive"));
        mvc.perform(get("/api/v1/posts/same-time-b"))
            .andExpect(jsonPath("$.previousPost.slug").value("same-time-a"))
            .andExpect(jsonPath("$.nextPost").value(nullValue()));
    }

    private void insertPost(String id, String slug, String status, String publishedAt, String... tagIds) {
        String title = Character.toUpperCase(slug.charAt(0)) + slug.substring(1);
        jdbc.update("""
            insert into posts (id, slug, title, summary, content, status, published_at, created_at, updated_at)
            values (?, ?, ?, ?, 'body', ?, cast(? as timestamp with time zone), now(), now())
            """, UUID.fromString(id), slug, title, "Summary of " + slug, status, publishedAt);
        for (String tagId : tagIds) {
            jdbc.update("insert into post_tags (post_id, tag_id) values (?, ?)",
                UUID.fromString(id), UUID.fromString(tagId));
        }
    }
}
