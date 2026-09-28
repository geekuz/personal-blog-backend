package com.personalblog.media;

import java.time.Instant;
import java.util.UUID;

public record MediaUploadResponse(
    UUID id,
    String url,
    String publicId,
    String originalFilename,
    String contentType,
    long sizeBytes,
    int width,
    int height,
    Instant createdAt
) {}
