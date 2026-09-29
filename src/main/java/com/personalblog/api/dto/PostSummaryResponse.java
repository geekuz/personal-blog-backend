package com.personalblog.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** {@code snippet} is only present in search results, as plain text around the first match in the body. */
public record PostSummaryResponse(String slug, String title, String summary,
                                  String coverImageUrl, String coverImageAlt, List<String> tags,
                                  Instant publishedAt, int readingTimeMinutes,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) String snippet) {}
