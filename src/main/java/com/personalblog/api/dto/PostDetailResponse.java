package com.personalblog.api.dto;

import java.time.Instant;
import java.util.List;

public record PostDetailResponse(String slug, String title, String summary, String content, List<String> tags,
                                 Instant publishedAt, Instant updatedAt, int readingTimeMinutes) {}
