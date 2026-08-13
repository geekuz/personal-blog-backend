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
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private PostStatus status;
    private Instant publishedAt;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @OrderBy("name asc")
    private Set<Tag> tags = new LinkedHashSet<>();

    protected Post() {}
    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }
    public PostStatus getStatus() { return status; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<Tag> getTags() { return tags; }
}
