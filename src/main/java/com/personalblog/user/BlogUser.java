package com.personalblog.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class BlogUser {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true, length = 254) private String email;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Column(name = "display_name", nullable = false, length = 80) private String displayName;
    @Column(nullable = false) private boolean enabled;
    @Column(name = "email_verified", nullable = false) private boolean emailVerified;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Set<UserRole> roles = new LinkedHashSet<>();

    protected BlogUser() {}

    public BlogUser(String email, String passwordHash, String displayName, Instant now) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.enabled = true;
        this.emailVerified = false;
        this.createdAt = now;
        this.updatedAt = now;
        this.roles.add(UserRole.USER);
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public boolean isEnabled() { return enabled; }
    public boolean isEmailVerified() { return emailVerified; }
    public Set<UserRole> getRoles() { return Set.copyOf(roles); }

    public void verifyEmail(Instant now) {
        this.emailVerified = true;
        this.updatedAt = now;
    }

    public void changePassword(String newPasswordHash, Instant now) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = now;
    }

    public void grantRole(UserRole role, Instant now) {
        if (roles.add(role)) updatedAt = now;
    }
}
