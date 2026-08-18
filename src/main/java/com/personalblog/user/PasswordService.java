package com.personalblog.user;

import com.personalblog.email.EmailDeliveryException;
import com.personalblog.email.PasswordResetEmailSender;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PasswordService {
    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final Duration EMAIL_COOLDOWN = Duration.ofMinutes(1);

    private final BlogUserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordResetEmailSender sender;
    private final PasswordEncoder passwords;
    private final ApplicationEventPublisher events;
    private final SecureRandom random = new SecureRandom();
    private final String frontendUrl;

    public PasswordService(BlogUserRepository users, PasswordResetTokenRepository tokens,
                           PasswordResetEmailSender sender, PasswordEncoder passwords,
                           ApplicationEventPublisher events,
                           @Value("${blog.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.users = users;
        this.tokens = tokens;
        this.sender = sender;
        this.passwords = passwords;
        this.events = events;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public void requestReset(String email) {
        Optional<BlogUser> account = users.findByEmail(normalize(email));
        if (account.isEmpty()) return;

        BlogUser user = account.get();
        Instant now = Instant.now();
        boolean coolingDown = tokens.findTopByUserIdOrderByCreatedAtDesc(user.getId())
            .map(latest -> latest.getCreatedAt().isAfter(now.minus(EMAIL_COOLDOWN)))
            .orElse(false);
        if (coolingDown) return;

        tokens.deleteByUserId(user.getId());
        String rawToken = newToken();
        PasswordResetToken token = tokens.save(
            new PasswordResetToken(user, hash(rawToken), now.plus(TOKEN_LIFETIME), now));
        String url = frontendUrl + "/reset-password?token="
            + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        try {
            sender.sendReset(user.getEmail(), user.getDisplayName(), url);
        } catch (EmailDeliveryException ex) {
            // Recovery must not reveal whether an account exists or whether delivery failed.
            tokens.delete(token);
            log.warn("Password reset email delivery failed: {}", ex.getClass().getSimpleName());
        }
    }

    public void reset(String rawToken, String newPassword) {
        PasswordResetToken token = tokens.findByTokenHash(hash(rawToken))
            .orElseThrow(InvalidPasswordResetTokenException::new);
        Instant now = Instant.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new InvalidPasswordResetTokenException();
        }
        token.consume(now);
        token.getUser().changePassword(passwords.encode(newPassword), now);
        events.publishEvent(new UserPasswordChangedEvent(token.getUser().getEmail()));
    }

    public void change(String email, String currentPassword, String newPassword) {
        BlogUser user = users.findByEmail(normalize(email))
            .orElseThrow(IncorrectCurrentPasswordException::new);
        if (!passwords.matches(currentPassword, user.getPasswordHash())) {
            throw new IncorrectCurrentPasswordException();
        }
        user.changePassword(passwords.encode(newPassword), Instant.now());
        tokens.deleteByUserId(user.getId());
        events.publishEvent(new UserPasswordChangedEvent(user.getEmail()));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
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
