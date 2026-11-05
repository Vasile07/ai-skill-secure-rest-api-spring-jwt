package com.vasile07.readingtracker.auth;

import com.vasile07.readingtracker.common.ConflictException;
import com.vasile07.readingtracker.security.JwtService;
import com.vasile07.readingtracker.user.User;
import com.vasile07.readingtracker.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final RefreshTokenRepository tokens;
    private final long refreshDays;
    private final long accessMinutes;
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            UserRepository users,
            PasswordEncoder passwords,
            JwtService jwt,
            RefreshTokenRepository tokens,
            @Value("${app.jwt.refresh-ttl-days:7}") long refreshDays,
            @Value("${app.jwt.access-ttl-minutes:15}") long accessMinutes) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
        this.tokens = tokens;
        this.refreshDays = refreshDays;
        this.accessMinutes = accessMinutes;
    }

    public AuthDtos.TokenResponse register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.findByEmail(email).isPresent()) {
            throw new ConflictException("Email is already registered");
        }

        User user = users.save(new User(
                UUID.randomUUID(),
                request.name().trim(),
                email,
                passwords.encode(request.password()),
                List.of("USER")));
        return issue(user, UUID.randomUUID());
    }

    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(email).orElse(null);
        if (user == null
                || user.passwordHash() == null
                || user.passwordHash().isBlank()
                || !passwords.matches(request.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return issue(user, UUID.randomUUID());
    }

    public synchronized AuthDtos.TokenResponse refresh(String raw) {
        RefreshTokenRepository.Token token = tokens.find(hash(raw))
                .orElseThrow(InvalidCredentialsException::new);
        if (token.used() || token.revoked()) {
            tokens.revokeFamily(token.family());
            throw new InvalidCredentialsException();
        }
        if (token.expires().isBefore(Instant.now())) {
            throw new InvalidCredentialsException();
        }

        tokens.save(new RefreshTokenRepository.Token(
                token.hash(), token.user(), token.family(), token.expires(), true, token.revoked()));
        User user = users.findById(token.user()).orElseThrow(InvalidCredentialsException::new);
        return issue(user, token.family());
    }

    public void logout(String raw) {
        tokens.find(hash(raw)).ifPresent(token -> tokens.revokeFamily(token.family()));
    }

    private AuthDtos.TokenResponse issue(User user, UUID family) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(refreshDays, ChronoUnit.DAYS);
        tokens.save(new RefreshTokenRepository.Token(
                hash(raw), user.id(), family, expiresAt, false, false));

        return new AuthDtos.TokenResponse(
                jwt.access(user), jwt.identity(user), raw, accessMinutes * 60);
    }

    private String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException { }
}
