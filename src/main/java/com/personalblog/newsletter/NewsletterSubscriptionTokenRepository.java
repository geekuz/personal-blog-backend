package com.personalblog.newsletter;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterSubscriptionTokenRepository
        extends JpaRepository<NewsletterSubscriptionToken, UUID> {
    Optional<NewsletterSubscriptionToken> findByTokenHashAndPurpose(
        String tokenHash, NewsletterTokenPurpose purpose);
    Optional<NewsletterSubscriptionToken> findTopBySubscriptionIdAndPurposeOrderByCreatedAtDesc(
        UUID subscriptionId, NewsletterTokenPurpose purpose);
    void deleteBySubscriptionIdAndPurpose(UUID subscriptionId, NewsletterTokenPurpose purpose);
}
