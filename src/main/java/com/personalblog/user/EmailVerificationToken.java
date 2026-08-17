package com.personalblog.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private BlogUser user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "used_at") private Instant usedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected EmailVerificationToken() {}

    public EmailVerificationToken(BlogUser user, String tokenHash, Instant expiresAt, Instant createdAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public BlogUser getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void consume(Instant now) { this.usedAt = now; }
}
