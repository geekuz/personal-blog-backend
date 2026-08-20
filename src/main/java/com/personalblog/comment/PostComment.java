package com.personalblog.comment;

import com.personalblog.post.Post;
import com.personalblog.user.BlogUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post_comments")
public class PostComment {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false) private Post post;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private BlogUser author;
    @Column(nullable = false, length = 2000) private String body;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected PostComment() {}

    public PostComment(Post post, BlogUser author, String body, Instant createdAt) {
        this.post = post;
        this.author = author;
        this.body = body;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public BlogUser getAuthor() { return author; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
