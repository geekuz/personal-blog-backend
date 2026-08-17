package com.personalblog.user;

import java.time.Instant;
import java.util.Locale;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BlogUserService implements UserDetailsService {
    private final BlogUserRepository users;
    private final PasswordEncoder passwords;

    public BlogUserService(BlogUserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    public BlogUser register(String email, String rawPassword, String displayName) {
        String normalizedEmail = normalize(email);
        if (users.existsByEmail(normalizedEmail)) throw new EmailAlreadyRegisteredException();
        return users.save(new BlogUser(normalizedEmail, passwords.encode(rawPassword), displayName.trim(), Instant.now()));
    }

    @Transactional(readOnly = true)
    public BlogUser byEmail(String email) {
        return users.findByEmail(normalize(email)).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        BlogUser user = byEmail(email);
        String[] authorities = user.getRoles().stream().map(role -> "ROLE_" + role.name()).toArray(String[]::new);
        return User.withUsername(user.getEmail()).password(user.getPasswordHash())
            .authorities(authorities).disabled(!user.isEnabled()).build();
    }

    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
