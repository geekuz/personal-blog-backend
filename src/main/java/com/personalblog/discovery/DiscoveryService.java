package com.personalblog.discovery;

import com.personalblog.api.SlugFormat;
import com.personalblog.post.PostRepository;
import com.personalblog.post.PostStatus;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the RSS feed, sitemap, and link-preview pages. Only published posts are ever exposed. */
@Service
@Transactional(readOnly = true)
public class DiscoveryService {
    static final int FEED_SIZE = 20;
    // Well under the 50,000-URL sitemap limit; a personal blog will not approach it.
    static final int SITEMAP_LIMIT = 5000;
    private static final Pattern SLUG = Pattern.compile(SlugFormat.PATTERN);

    private final PostRepository posts;
    private final SiteInfo site;

    public DiscoveryService(PostRepository posts,
                            @Value("${blog.frontend-url:http://localhost:5173}") String frontendUrl,
                            @Value("${blog.site.title}") String title,
                            @Value("${blog.site.description}") String description) {
        this.posts = posts;
        this.site = new SiteInfo(frontendUrl, title, description);
    }

    public String rssFeed() {
        return FeedXmlWriter.rss(site,
            posts.findByStatusOrderByPublishedAtDescIdDesc(PostStatus.PUBLISHED, PageRequest.of(0, FEED_SIZE)));
    }

    public String sitemap() {
        return FeedXmlWriter.sitemap(site, posts.findPublishedRefs(PageRequest.of(0, SITEMAP_LIMIT)));
    }

    public Optional<String> sharePage(String slug) {
        if (slug == null || !SLUG.matcher(slug).matches()) return Optional.empty();
        return posts.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
            .map(post -> SharePageRenderer.render(site, post));
    }

    public String notFoundPage() {
        return SharePageRenderer.notFound(site);
    }
}
