package com.personalblog.newsletter;

import com.personalblog.email.NewsletterConfirmationEmailSender;
import com.personalblog.email.EmailDeliveryException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class PublicNewsletterService {
    private static final Logger log = LoggerFactory.getLogger(PublicNewsletterService.class);
    private final NewsletterSubscriptionRepository subscriptions;
    private final NewsletterTokenService tokens;
    private final NewsletterConfirmationEmailSender sender;
    private final String frontendUrl;

    public PublicNewsletterService(NewsletterSubscriptionRepository subscriptions,
            NewsletterTokenService tokens, NewsletterConfirmationEmailSender sender,
            @Value("${blog.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.subscriptions = subscriptions;
        this.tokens = tokens;
        this.sender = sender;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public void requestSubscription(String requestedEmail) {
        String email = requestedEmail.strip().toLowerCase(Locale.ROOT);
        Instant now = Instant.now();
        NewsletterSubscription subscription = subscriptions.findByEmailIgnoreCase(email)
            .orElseGet(() -> subscriptions.saveAndFlush(new NewsletterSubscription(email, now)));
        if (subscription.isConfirmed() || !tokens.canIssueConfirmation(subscription, now)) return;

        String rawToken = tokens.issueConfirmation(subscription, now);
        String url = frontendUrl + "/newsletter/confirm?token=" + encode(rawToken);
        try {
            sender.sendConfirmation(subscription.getEmail(), url);
        } catch (EmailDeliveryException ex) {
            log.warn("Newsletter confirmation delivery failed");
        }
    }

    public void confirm(String token) {
        tokens.confirm(token, Instant.now());
    }

    public void unsubscribe(String token) {
        tokens.unsubscribe(token, Instant.now());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
