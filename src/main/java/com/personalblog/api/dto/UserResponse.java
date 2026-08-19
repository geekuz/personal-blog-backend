package com.personalblog.api.dto;

import com.personalblog.user.UserRole;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String displayName,
    boolean emailVerified,
    Set<UserRole> roles
) {}
