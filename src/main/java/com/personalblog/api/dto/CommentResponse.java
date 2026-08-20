package com.personalblog.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    String authorDisplayName,
    String body,
    Instant createdAt,
    boolean canDelete
) {}
