package com.vasile07.readingtracker.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() { }
    public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email, @NotBlank String password) { }
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }
    public record RefreshRequest(@NotBlank String refreshToken) { }
    public record TokenResponse(String accessToken, String idToken, String refreshToken, long expiresIn) { }
}
