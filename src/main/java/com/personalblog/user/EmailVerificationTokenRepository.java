package com.personalblog.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    Optional<EmailVerificationToken> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
    void deleteByUserId(UUID userId);
}
