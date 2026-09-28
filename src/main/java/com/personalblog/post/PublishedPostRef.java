package com.personalblog.post;

import java.time.Instant;

/** Lightweight view of a published post for sitemap generation. */
public record PublishedPostRef(String slug, Instant updatedAt) {}
