package com.personalblog.newsletter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NewsletterTokenService {
    private static final Duration CONFIRMATION_LIFETIME = Duration.ofHours(24);
    private static final Duration UNSUBSCRIBE_LIFETIME = Duration.ofDays(365);
    private static final Duration CONFIRMATION_COOLDOWN = Duration.ofMinutes(1);

    private final NewsletterSubscriptionTokenRepository tokens;
    private final NewsletterSubscriptionRepository subscriptions;
    private final SecureRandom random = new SecureRandom();

    public NewsletterTokenService(NewsletterSubscriptionTokenRepository tokens,
            NewsletterSubscriptionRepository subscriptions) {
        this.tokens = tokens;
        this.subscriptions = subscriptions;
    }

    public String issueConfirmation(NewsletterSubscription subscription, Instant now) {
        tokens.deleteBySubscriptionIdAndPurpose(subscription.getId(), NewsletterTokenPurpose.CONFIRM);
        return issue(subscription, NewsletterTokenPurpose.CONFIRM, now.plus(CONFIRMATION_LIFETIME), now);
    }

    public boolean canIssueConfirmation(NewsletterSubscription subscription, Instant now) {
        return tokens.findTopBySubscriptionIdAndPurposeOrderByCreatedAtDesc(
                subscription.getId(), NewsletterTokenPurpose.CONFIRM)
            .map(token -> !token.getCreatedAt().isAfter(now.minus(CONFIRMATION_COOLDOWN)))
            .orElse(true);
    }

    public void confirm(String rawToken, Instant now) {
        NewsletterSubscriptionToken token = valid(rawToken, NewsletterTokenPurpose.CONFIRM, now);
        token.consume(now);
        token.getSubscription().confirm(now);
    }

    public String issueUnsubscribe(NewsletterSubscription subscription, Instant now) {
        return issue(subscription, NewsletterTokenPurpose.UNSUBSCRIBE, now.plus(UNSUBSCRIBE_LIFETIME), now);
    }

    public void unsubscribe(String rawToken, Instant now) {
        NewsletterSubscriptionToken token = valid(rawToken, NewsletterTokenPurpose.UNSUBSCRIBE, now);
        token.consume(now);
        subscriptions.delete(token.getSubscription());
    }

    private String issue(NewsletterSubscription subscription, NewsletterTokenPurpose purpose,
            Instant expiresAt, Instant now) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new NewsletterSubscriptionToken(
            subscription, purpose, hash(rawToken), expiresAt, now));
        return rawToken;
    }

    private NewsletterSubscriptionToken valid(String rawToken, NewsletterTokenPurpose purpose, Instant now) {
        NewsletterSubscriptionToken token = tokens.findByTokenHashAndPurpose(hash(rawToken), purpose)
            .orElseThrow(InvalidNewsletterTokenException::new);
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new InvalidNewsletterTokenException();
        }
        return token;
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
