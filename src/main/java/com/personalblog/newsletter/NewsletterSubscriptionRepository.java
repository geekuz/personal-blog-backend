package com.personalblog.newsletter;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterSubscriptionRepository extends JpaRepository<NewsletterSubscription, UUID> {
    boolean existsByUserId(UUID userId);
    void deleteByUserId(UUID userId);
    Optional<NewsletterSubscription> findByUserId(UUID userId);
    Optional<NewsletterSubscription> findByEmailIgnoreCase(String email);
    List<NewsletterSubscription> findAllByConfirmedAtIsNotNullOrderBySubscribedAtAsc();
    long countByConfirmedAtIsNotNull();
}
