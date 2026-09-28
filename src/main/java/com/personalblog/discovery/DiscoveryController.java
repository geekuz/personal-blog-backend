package com.personalblog.discovery;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves feed, sitemap, and preview pages outside /api/v1 because they are consumed as documents, not JSON.
 * The frontend proxies these paths so crawlers see them on the reader-facing domain.
 */
@RestController
public class DiscoveryController {
    // s-maxage lets the proxying CDN absorb repeat feed/sitemap traffic while this service is cold-starting.
    private static final CacheControl PUBLIC_CACHE =
        CacheControl.maxAge(Duration.ofMinutes(10)).sMaxAge(Duration.ofHours(1)).cachePublic();
    // The frontend serves /blog/{slug} to readers as the SPA and to preview bots as this page, keyed only by
    // User-Agent. A shared cache keyed by path could hand one to the other, so share pages stay out of it.
    private static final CacheControl SHARE_CACHE = CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate();
    private static final CacheControl NOT_FOUND_CACHE = CacheControl.maxAge(Duration.ofMinutes(1)).cachePrivate();
    private static final MediaType RSS = new MediaType("application", "rss+xml", StandardCharsets.UTF_8);
    private static final MediaType XML = new MediaType("application", "xml", StandardCharsets.UTF_8);
    private static final MediaType HTML = new MediaType("text", "html", StandardCharsets.UTF_8);
    private static final String SHARE_PAGE_CSP = "default-src 'none'; img-src https: http:";

    private final DiscoveryService discovery;

    public DiscoveryController(DiscoveryService discovery) { this.discovery = discovery; }

    @GetMapping("/feed.xml")
    public ResponseEntity<String> feed() {
        return ResponseEntity.ok().cacheControl(PUBLIC_CACHE).contentType(RSS).body(discovery.rssFeed());
    }

    @GetMapping("/sitemap.xml")
    public ResponseEntity<String> sitemap() {
        return ResponseEntity.ok().cacheControl(PUBLIC_CACHE).contentType(XML).body(discovery.sitemap());
    }

    @GetMapping("/share/blog/{slug}")
    public ResponseEntity<String> share(@PathVariable String slug) {
        return discovery.sharePage(slug)
            .map(html -> sharePage(HttpStatus.OK, SHARE_CACHE, html))
            .orElseGet(() -> sharePage(HttpStatus.NOT_FOUND, NOT_FOUND_CACHE, discovery.notFoundPage()));
    }

    private static ResponseEntity<String> sharePage(HttpStatus status, CacheControl cache, String html) {
        return ResponseEntity.status(status).cacheControl(cache).varyBy(HttpHeaders.USER_AGENT).contentType(HTML)
            .header("Content-Security-Policy", SHARE_PAGE_CSP).body(html);
    }
}
