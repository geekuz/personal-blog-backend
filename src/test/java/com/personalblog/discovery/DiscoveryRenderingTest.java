package com.personalblog.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.personalblog.post.Post;
import com.personalblog.post.PostStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DiscoveryRenderingTest {
    private static final SiteInfo SITE = new SiteInfo("https://blog.example.com//", "Site", "About the site");
    private static final Instant PUBLISHED = Instant.parse("2026-09-01T12:00:00Z");

    @Test void siteUrlsIgnoreTrailingSlashes() {
        assertThat(SITE.pageUrl("/")).isEqualTo("https://blog.example.com/");
        assertThat(SITE.postUrl("hello")).isEqualTo("https://blog.example.com/blog/hello");
    }

    @Test void sharePageOmitsNonWebCoverImages() {
        for (String cover : new String[] {"javascript:alert(1)", "data:image/png;base64,AAAA", "/relative.png"}) {
            String html = SharePageRenderer.render(SITE, post("Title", cover));
            assertThat(html).doesNotContain("og:image").contains("content=\"summary\"");
        }
    }

    @Test void sharePageEscapesEveryHtmlSpecialCharacter() {
        assertThat(SharePageRenderer.escape("<a href=\"x\">'&'</a>"))
            .isEqualTo("&lt;a href=&quot;x&quot;&gt;&#39;&amp;&#39;&lt;/a&gt;");
    }

    @Test void feedDropsCharactersXmlCannotRepresent() {
        String xml = FeedXmlWriter.rss(SITE, List.of(post("Bad\u0000\u0008Title", null)));
        assertThat(xml).contains("<title>BadTitle</title>").doesNotContain("\u0000");
    }

    @Test void emptyFeedIsStillValidChannel() {
        String xml = FeedXmlWriter.rss(SITE, List.of());
        assertThat(xml).contains("<channel>").doesNotContain("<item>").doesNotContain("lastBuildDate");
    }

    private static Post post(String title, String cover) {
        return new Post("slug", title, "Summary", "body", cover, cover == null ? null : "alt",
            PostStatus.PUBLISHED, PUBLISHED, null, PUBLISHED, Set.of());
    }
}
