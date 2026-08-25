package com.personalblog.api.dto;

import com.personalblog.post.PostStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminPostResponse(
    UUID id,
    String slug,
    String title,
    String summary,
    String content,
    String coverImageUrl,
    String coverImageAlt,
    PostStatus status,
    Instant publishedAt,
    Instant scheduledAt,
    Instant createdAt,
    Instant updatedAt,
    List<TagInput> tags
) {}
