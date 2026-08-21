package com.personalblog.newsletter;

import com.personalblog.post.Post;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "newsletter_deliveries", uniqueConstraints =
    @UniqueConstraint(name = "uq_newsletter_deliveries_post_subscription", columnNames = {"post_id", "subscription_id"}))
public class NewsletterDelivery {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "post_id", nullable = false) private Post post;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subscription_id", nullable = false) private NewsletterSubscription subscription;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private NewsletterDeliveryStatus status;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "sent_at") private Instant sentAt;
    @Column(name = "last_error", length = 500) private String lastError;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected NewsletterDelivery() {}
    public NewsletterDelivery(Post post, NewsletterSubscription subscription, Instant now) {
        this.post = post; this.subscription = subscription; this.status = NewsletterDeliveryStatus.PENDING;
        this.nextAttemptAt = now; this.createdAt = now;
    }
    public UUID getId() { return id; }
    public Post getPost() { return post; }
    public NewsletterSubscription getSubscription() { return subscription; }
    public NewsletterDeliveryStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public void markSending() { status = NewsletterDeliveryStatus.SENDING; attempts++; }
    public void markSent(Instant now) { status = NewsletterDeliveryStatus.SENT; sentAt = now; lastError = null; }
    public void retry(Instant at, String error) { status = attempts >= 5 ? NewsletterDeliveryStatus.FAILED : NewsletterDeliveryStatus.PENDING; nextAttemptAt = at; lastError = error; }
}
