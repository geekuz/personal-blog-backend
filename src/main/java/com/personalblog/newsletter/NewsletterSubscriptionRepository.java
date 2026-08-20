package com.personalblog.newsletter;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterSubscriptionRepository extends JpaRepository<NewsletterSubscription, UUID> {
    boolean existsByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
