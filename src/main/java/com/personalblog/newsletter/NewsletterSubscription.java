package com.personalblog.newsletter;

import com.personalblog.user.BlogUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "newsletter_subscriptions")
public class NewsletterSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private BlogUser user;

    @Column(name = "subscribed_at", nullable = false, updatable = false)
    private Instant subscribedAt;

    protected NewsletterSubscription() {}

    public NewsletterSubscription(BlogUser user, Instant subscribedAt) {
        this.user = user;
        this.subscribedAt = subscribedAt;
    }

    public UUID getId() { return id; }
    public BlogUser getUser() { return user; }
}
