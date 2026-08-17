package com.personalblog.api.dto;

import com.personalblog.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotBlank @Size(max = 80) String displayName) {}

    public record LoginRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 72) String password) {}

    public record UserResponse(UUID id, String email, String displayName, boolean emailVerified, Set<UserRole> roles) {}
    public record SessionResponse(boolean authenticated, UserResponse user) {}
    public record CsrfResponse(String headerName, String token) {}
    public record VerifyEmailRequest(@NotBlank @Size(max = 200) String token) {}
}
