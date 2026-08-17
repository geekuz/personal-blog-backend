package com.personalblog.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogUserRepository extends JpaRepository<BlogUser, UUID> {
    Optional<BlogUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
