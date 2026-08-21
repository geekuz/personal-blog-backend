package com.personalblog.user;

import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminRoleBootstrap implements ApplicationRunner {
    private final BlogUserRepository users;
    private final String adminEmail;
    public AdminRoleBootstrap(BlogUserRepository users, @Value("${blog.admin.email:}") String adminEmail) {
        this.users = users; this.adminEmail = adminEmail;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank()) return;
        users.findByEmail(adminEmail.trim().toLowerCase(Locale.ROOT))
            .ifPresent(user -> user.grantRole(UserRole.ADMIN, Instant.now()));
    }
}
