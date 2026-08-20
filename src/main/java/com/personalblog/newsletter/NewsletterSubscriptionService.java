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
        return subscriptions.existsByUserId(user.getId());
    }

    public void subscribe(String email) {
        BlogUser user = verifiedUser(email);
        if (!subscriptions.existsByUserId(user.getId())) {
            subscriptions.save(new NewsletterSubscription(user, Instant.now()));
        }
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
