package com.vasile07.readingtracker.auth;

import com.vasile07.readingtracker.user.User;
import com.vasile07.readingtracker.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;
    private final UserRepository users;

    public AuthController(AuthService auth, UserRepository users) {
        this.auth = auth;
        this.users = users;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.TokenResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public AuthDtos.TokenResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return auth.login(request);
    }

    @PostMapping("/refresh")
    public AuthDtos.TokenResponse refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return auth.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        auth.logout(request.refreshToken());
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public User me(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return users.findById(userId).orElseThrow();
    }
}
