package com.requirementanalyzer.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 72) String password,
            @NotBlank @Size(max = 120) String displayName
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password
    ) {
    }

    public record UpdateProfileRequest(@NotBlank @Size(max = 120) String displayName) {
    }

    public record UserProfile(UUID id, String email, String displayName, String status) {
    }

    public record AuthResponse(String accessToken, Instant expiresAt, UserProfile user) {
    }

    public record AuthResult(AuthResponse response, String refreshToken) {
    }
}