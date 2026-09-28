package com.personalblog.discovery;

/** Public identity of the reader-facing site; all generated links point at the frontend, not the API. */
record SiteInfo(String baseUrl, String title, String description) {
    SiteInfo {
        baseUrl = baseUrl.replaceAll("/+$", "");
    }

    String pageUrl(String path) { return baseUrl + path; }

    String postUrl(String slug) { return baseUrl + "/blog/" + slug; }
}
