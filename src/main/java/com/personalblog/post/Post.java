package com.personalblog.post;

import com.personalblog.tag.Tag;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "posts")
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true, length = 160) private String slug;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, length = 500) private String summary;
    @Column(nullable = false, columnDefinition = "text") private String content;
    @Column(length = 2048) private String coverImageUrl;
    @Column(length = 300) private String coverImageAlt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private PostStatus status;
    private Instant publishedAt;
    private Instant scheduledAt;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @OrderBy("name asc")
    private Set<Tag> tags = new LinkedHashSet<>();

    protected Post() {}
    public Post(String slug, String title, String summary, String content, String coverImageUrl,
                String coverImageAlt, PostStatus status,
                Instant publishedAt, Instant scheduledAt, Instant now, Set<Tag> tags) {
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.coverImageUrl = coverImageUrl;
        this.coverImageAlt = coverImageAlt;
        this.status = status;
        this.publishedAt = publishedAt;
        this.scheduledAt = scheduledAt;
        this.createdAt = now;
        this.updatedAt = now;
        this.tags = new LinkedHashSet<>(tags);
    }

    public void update(String slug, String title, String summary, String content, String coverImageUrl,
                       String coverImageAlt, PostStatus status,
                       Instant publishedAt, Instant scheduledAt, Instant now, Set<Tag> tags) {
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.coverImageUrl = coverImageUrl;
        this.coverImageAlt = coverImageAlt;
        this.status = status;
        this.publishedAt = publishedAt;
        this.scheduledAt = scheduledAt;
        this.updatedAt = now;
        this.tags.clear();
        this.tags.addAll(tags);
    }
    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public String getCoverImageAlt() { return coverImageAlt; }
    public PostStatus getStatus() { return status; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<Tag> getTags() { return tags; }

    public void publish(Instant now) {
        this.status = PostStatus.PUBLISHED;
        this.publishedAt = now;
        this.scheduledAt = null;
        this.updatedAt = now;
    }
}
