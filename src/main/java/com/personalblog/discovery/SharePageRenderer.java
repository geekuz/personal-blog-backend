package com.personalblog.discovery;

import com.personalblog.post.Post;
import com.personalblog.tag.Tag;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Minimal HTML carrying Open Graph and Twitter card metadata for link-preview bots. The SPA cannot serve
 * these because preview bots do not run JavaScript. Every interpolated value is HTML-escaped, and the page
 * contains no scripts, so post content cannot inject markup.
 */
final class SharePageRenderer {
    private SharePageRenderer() {}

    static String render(SiteInfo site, Post post) {
        String url = site.postUrl(post.getSlug());
        String image = httpUrlOrNull(post.getCoverImageUrl());
        StringBuilder html = new StringBuilder(2048);
        html.append("<!doctype html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n")
            .append("<title>").append(escape(post.getTitle())).append(" — ").append(escape(site.title()))
            .append("</title>\n");
        meta(html, "name", "description", post.getSummary());
        html.append("<link rel=\"canonical\" href=\"").append(escape(url)).append("\">\n");
        meta(html, "property", "og:type", "article");
        meta(html, "property", "og:site_name", site.title());
        meta(html, "property", "og:title", post.getTitle());
        meta(html, "property", "og:description", post.getSummary());
        meta(html, "property", "og:url", url);
        if (image != null) {
            meta(html, "property", "og:image", image);
            String alt = post.getCoverImageAlt();
            if (alt != null && !alt.isBlank()) meta(html, "property", "og:image:alt", alt);
        }
        meta(html, "name", "twitter:card", image == null ? "summary" : "summary_large_image");
        meta(html, "property", "article:published_time", post.getPublishedAt().toString());
        for (Tag tag : post.getTags()) meta(html, "property", "article:tag", tag.getName());
        return html.append("</head>\n<body>\n<main>\n<h1>").append(escape(post.getTitle())).append("</h1>\n<p>")
            .append(escape(post.getSummary())).append("</p>\n<p><a href=\"").append(escape(url))
            .append("\">Read the post</a></p>\n</main>\n</body>\n</html>\n")
            .toString();
    }

    static String notFound(SiteInfo site) {
        return "<!doctype html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n<title>Post not found — "
            + escape(site.title()) + "</title>\n</head>\n<body>\n<p>Post not found.</p>\n</body>\n</html>\n";
    }

    static String escape(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static void meta(StringBuilder html, String keyAttribute, String key, String content) {
        html.append("<meta ").append(keyAttribute).append("=\"").append(key).append("\" content=\"")
            .append(escape(content)).append("\">\n");
    }

    // Only absolute http(s) URLs are useful to preview bots; anything else is omitted rather than echoed.
    private static String httpUrlOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            boolean web = "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme);
            return web && uri.getHost() != null ? uri.toString() : null;
        } catch (URISyntaxException ex) {
            return null;
        }
    }
}
