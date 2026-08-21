package com.personalblog.newsletter;

import com.personalblog.email.EmailDeliveryException;
import com.personalblog.email.NewsletterEmailSender;
import com.personalblog.post.Post;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsletterDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(NewsletterDeliveryService.class);
    private final NewsletterDeliveryRepository deliveries;
    private final NewsletterSubscriptionRepository subscriptions;
    private final NewsletterEmailSender sender;

    public NewsletterDeliveryService(NewsletterDeliveryRepository deliveries,
            NewsletterSubscriptionRepository subscriptions, NewsletterEmailSender sender) {
        this.deliveries = deliveries; this.subscriptions = subscriptions; this.sender = sender;
    }

    @Transactional
    public void enqueue(Post post) {
        Instant now = Instant.now();
        List<NewsletterDelivery> pending = subscriptions.findAllByOrderBySubscribedAtAsc().stream()
            .filter(subscription -> !deliveries.existsByPostIdAndSubscriptionId(post.getId(), subscription.getId()))
            .map(subscription -> new NewsletterDelivery(post, subscription, now)).toList();
        deliveries.saveAll(pending);
    }

    @Scheduled(fixedDelayString = "${blog.newsletter.dispatch-delay-ms:15000}")
    @Transactional
    public void dispatch() {
        Instant now = Instant.now();
        List<NewsletterDelivery> batch = deliveries
            .findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                NewsletterDeliveryStatus.PENDING, now, PageRequest.of(0, 25));
        for (NewsletterDelivery delivery : batch) {
            delivery.markSending();
            try {
                var user = delivery.getSubscription().getUser();
                sender.send(delivery.getId(), user.getEmail(), user.getDisplayName(), delivery.getPost());
                delivery.markSent(Instant.now());
            } catch (EmailDeliveryException ex) {
                long minutes = Math.min(60, 1L << Math.min(delivery.getAttempts(), 5));
                delivery.retry(Instant.now().plus(Duration.ofMinutes(minutes)), safeMessage(ex));
                log.warn("Newsletter delivery {} failed on attempt {}", delivery.getId(), delivery.getAttempts());
            }
        }
    }

    private String safeMessage(EmailDeliveryException exception) {
        String message = exception.getMessage();
        return message == null ? "Email delivery failed" : message.substring(0, Math.min(500, message.length()));
    }
}
