package com.personalblog.api.dto;

import java.time.Instant;
import java.util.List;

public record PostSummaryResponse(String slug, String title, String summary, List<String> tags,
                                  Instant publishedAt, int readingTimeMinutes) {}
