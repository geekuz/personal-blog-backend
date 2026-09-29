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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {
    "classpath:reset-blog-data.sql",
    "classpath:db/migration/V2__seed_public_posts.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DiscoveryIntegrationTest {
    private static final String SITE = "http://localhost:5173";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void addPosts() {
        insertPost("30000000-0000-0000-0000-000000000001", "secret-draft", "Secret Draft", "Private text",
            null, "DRAFT", null);
        insertPost("30000000-0000-0000-0000-000000000002", "future-post", "Future Post", "Not yet public",
            null, "SCHEDULED", null);
        insertPost("30000000-0000-0000-0000-000000000003", "tricky-title",
            "Tags & <script>alert(\"x\")</script>", "Summary with 'quotes' & <b>markup</b>",
            "https://media.example.com/cover.png", "PUBLISHED", "2026-09-01T12:00:00Z");
    }

    @Test void feedListsPublishedPostsNewestFirstWithFrontendLinks() throws Exception {
        mvc.perform(get("/feed.xml"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", startsWith("application/rss+xml")))
            .andExpect(header().string("Cache-Control", containsString("max-age=")))
            .andExpect(xpath("/rss/channel/link").string(SITE + "/"))
            .andExpect(xpath("/rss/channel/item").nodeCount(4))
            .andExpect(xpath("/rss/channel/item[1]/link").string(SITE + "/blog/tricky-title"))
            .andExpect(xpath("/rss/channel/item[1]/title").string("Tags & <script>alert(\"x\")</script>"))
            .andExpect(xpath("/rss/channel/item[1]/pubDate").string("Tue, 1 Sep 2026 12:00:00 GMT"))
            .andExpect(xpath("/rss/channel/item[2]/link").string(SITE + "/blog/building-a-blog-api"))
            .andExpect(xpath("/rss/channel/item[4]/category[1]").string("Learning"))
            .andExpect(content().string(not(containsString("secret-draft"))))
            .andExpect(content().string(not(containsString("future-post"))));
    }

    @Test void sitemapListsStaticPagesAndPublishedPostsOnly() throws Exception {
        mvc.perform(get("/sitemap.xml"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", startsWith("application/xml")))
            .andExpect(content().string(containsString("<loc>" + SITE + "/</loc>")))
            .andExpect(content().string(containsString("<loc>" + SITE + "/projects</loc>")))
            .andExpect(content().string(containsString("<loc>" + SITE + "/projects/otabek-dev</loc>")))
            .andExpect(content().string(containsString("<loc>" + SITE + "/about</loc>")))
            .andExpect(content().string(containsString("<loc>" + SITE + "/contact</loc>")))
            .andExpect(content().string(containsString("<loc>" + SITE + "/blog/why-i-chose-react</loc>")))
            .andExpect(content().string(containsString("<lastmod>2026-09-01</lastmod>")))
            .andExpect(content().string(not(containsString("secret-draft"))))
            .andExpect(content().string(not(containsString("future-post"))));
    }

    @Test void sharePageExposesEscapedPreviewMetadata() throws Exception {
        mvc.perform(get("/share/blog/tricky-title"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", startsWith("text/html")))
            // Same URL serves the SPA to readers, so shared caches must never store this bot-only page.
            .andExpect(header().string("Cache-Control", allOf(containsString("private"), not(containsString("s-maxage")))))
            .andExpect(header().stringValues("Vary", hasItem(containsString("User-Agent"))))
            .andExpect(content().string(not(containsString("<script>"))))
            .andExpect(content().string(containsString(
                "<meta property=\"og:title\" content=\"Tags &amp; &lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;\">")))
            .andExpect(content().string(containsString(
                "<meta property=\"og:description\" content=\"Summary with &#39;quotes&#39; &amp; &lt;b&gt;markup&lt;/b&gt;\">")))
            .andExpect(content().string(containsString(
                "<meta property=\"og:url\" content=\"" + SITE + "/blog/tricky-title\">")))
            .andExpect(content().string(containsString(
                "<link rel=\"canonical\" href=\"" + SITE + "/blog/tricky-title\">")))
            .andExpect(content().string(containsString(
                "<meta property=\"og:image\" content=\"https://media.example.com/cover.png\">")))
            .andExpect(content().string(containsString(
                "<meta property=\"og:image:alt\" content=\"Cover &lt;alt&gt;\">")))
            .andExpect(content().string(containsString(
                "<meta name=\"twitter:card\" content=\"summary_large_image\">")));
    }

    @Test void sharePageWithoutCoverUsesSmallCard() throws Exception {
        mvc.perform(get("/share/blog/why-i-chose-react"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("og:image"))))
            .andExpect(content().string(containsString("<meta name=\"twitter:card\" content=\"summary\">")))
            .andExpect(content().string(containsString(
                "<meta property=\"article:published_time\" content=\"2026-06-18T09:00:00Z\">")));
    }

    @Test void sharePageHidesDraftsScheduledUnknownAndInvalidSlugs() throws Exception {
        for (String slug : new String[] {"secret-draft", "future-post", "missing-post", "Not_A_Slug"}) {
            mvc.perform(get("/share/blog/" + slug))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", startsWith("text/html")))
                .andExpect(content().string(not(containsString("Secret Draft"))));
        }
    }

    private void insertPost(String id, String slug, String title, String summary, String cover,
                            String status, String publishedAt) {
        String scheduledAt = "SCHEDULED".equals(status) ? "2099-01-01T00:00:00Z" : null;
        String coverAlt = cover == null ? null : "Cover <alt>";
        jdbc.update("""
            insert into posts (id, slug, title, summary, content, cover_image_url, cover_image_alt, status,
                               published_at, scheduled_at, created_at, updated_at)
            values (?, ?, ?, ?, 'body', ?, ?, ?, cast(? as timestamp with time zone),
                    cast(? as timestamp with time zone), now(), coalesce(cast(? as timestamp with time zone), now()))
            """, UUID.fromString(id), slug, title, summary, cover, coverAlt, status, publishedAt, scheduledAt,
            publishedAt);
    }
}
