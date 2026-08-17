package com.personalblog.user;

import com.personalblog.email.VerificationEmailSender;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmailVerificationService {
    private static final Duration TOKEN_LIFETIME = Duration.ofHours(24);
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    private final EmailVerificationTokenRepository tokens;
    private final VerificationEmailSender sender;
    private final SecureRandom random = new SecureRandom();
    private final String frontendUrl;

    public EmailVerificationService(EmailVerificationTokenRepository tokens, VerificationEmailSender sender,
                                    @Value("${blog.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.tokens = tokens;
        this.sender = sender;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public boolean issue(BlogUser user) {
        if (user.isEmailVerified()) return false;
        Instant now = Instant.now();
        tokens.findTopByUserIdOrderByCreatedAtDesc(user.getId()).ifPresent(latest -> {
            if (latest.getCreatedAt().isAfter(now.minus(RESEND_COOLDOWN))) {
                throw new VerificationRateLimitException();
            }
        });
        tokens.deleteByUserId(user.getId());
        String rawToken = newToken();
        tokens.save(new EmailVerificationToken(user, hash(rawToken), now.plus(TOKEN_LIFETIME), now));
        String url = frontendUrl + "/verify-email?token="
            + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        sender.send(user.getEmail(), user.getDisplayName(), url);
        return true;
    }

    public BlogUser verify(String rawToken) {
        EmailVerificationToken token = tokens.findByTokenHash(hash(rawToken))
            .orElseThrow(InvalidVerificationTokenException::new);
        Instant now = Instant.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new InvalidVerificationTokenException();
        }
        token.consume(now);
        token.getUser().verifyEmail(now);
        return token.getUser();
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
