package com.personalblog.newsletter;

import com.personalblog.user.BlogUser;
import com.personalblog.user.BlogUserService;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NewsletterSubscriptionService {
    private final NewsletterSubscriptionRepository subscriptions;
    private final BlogUserService users;

    public NewsletterSubscriptionService(NewsletterSubscriptionRepository subscriptions,
                                         BlogUserService users) {
        this.subscriptions = subscriptions;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public boolean isSubscribed(String email) {
        BlogUser user = verifiedUser(email);
        return subscriptions.findByUserId(user.getId())
            .map(NewsletterSubscription::isConfirmed)
            .orElse(false);
    }

    public void subscribe(String email) {
        BlogUser user = verifiedUser(email);
        Instant now = Instant.now();
        NewsletterSubscription subscription = subscriptions.findByEmailIgnoreCase(user.getEmail())
            .orElseGet(() -> new NewsletterSubscription(user, now));
        if (subscription.getUser() == null || !subscription.isConfirmed()) {
            subscription.attachUser(user, now);
        }
        subscriptions.save(subscription);
    }

    public void unsubscribe(String email) {
        BlogUser user = verifiedUser(email);
        subscriptions.deleteByUserId(user.getId());
    }

    private BlogUser verifiedUser(String email) {
        BlogUser user = users.byEmail(email);
        if (!user.isEmailVerified()) throw new EmailVerificationRequiredException();
        return user;
    }
}
