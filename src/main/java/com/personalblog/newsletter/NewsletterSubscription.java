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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private BlogUser user;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "subscribed_at", nullable = false, updatable = false)
    private Instant subscribedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected NewsletterSubscription() {}

    public NewsletterSubscription(BlogUser user, Instant subscribedAt) {
        this.user = user;
        this.email = user.getEmail();
        this.displayName = user.getDisplayName();
        this.subscribedAt = subscribedAt;
        this.confirmedAt = subscribedAt;
    }

    public NewsletterSubscription(String email, Instant subscribedAt) {
        this.email = email;
        this.displayName = "Reader";
        this.subscribedAt = subscribedAt;
    }

    public UUID getId() { return id; }
    public BlogUser getUser() { return user; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public boolean isConfirmed() { return confirmedAt != null; }

    public void confirm(Instant now) { this.confirmedAt = now; }

    public void attachUser(BlogUser user, Instant now) {
        this.user = user;
        this.email = user.getEmail();
        this.displayName = user.getDisplayName();
        this.confirmedAt = now;
    }
}
