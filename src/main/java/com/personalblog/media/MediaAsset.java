package com.personalblog.media;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
public class MediaAsset {
    @Id private UUID id;
    @Column(name = "object_key", nullable = false, unique = true, length = 512) private String objectKey;
    @Column(name = "public_url", nullable = false, unique = true, length = 2048) private String publicUrl;
    @Column(name = "original_filename", nullable = false, length = 255) private String originalFilename;
    @Column(name = "content_type", nullable = false, length = 64) private String contentType;
    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
    @Column(nullable = false) private int width;
    @Column(nullable = false) private int height;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected MediaAsset() {}

    public MediaAsset(UUID id, String objectKey, String publicUrl, String originalFilename,
                      String contentType, long sizeBytes, int width, int height, Instant createdAt) {
        this.id = id;
        this.objectKey = objectKey;
        this.publicUrl = publicUrl;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getObjectKey() { return objectKey; }
    public String getPublicUrl() { return publicUrl; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Instant getCreatedAt() { return createdAt; }
}
