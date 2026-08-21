package com.personalblog.newsletter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterDeliveryRepository extends JpaRepository<NewsletterDelivery, UUID> {
    @EntityGraph(attributePaths = {"post", "subscription.user"})
    List<NewsletterDelivery> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
        NewsletterDeliveryStatus status, Instant now, Pageable pageable);
    long countByStatus(NewsletterDeliveryStatus status);
    boolean existsByPostIdAndSubscriptionId(UUID postId, UUID subscriptionId);
}
