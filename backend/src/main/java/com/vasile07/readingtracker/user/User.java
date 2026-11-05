package com.vasile07.readingtracker.user;

import java.util.UUID;

import java.util.List;

public record User(
        UUID id,
        String name,
        String email,
        @com.fasterxml.jackson.annotation.JsonIgnore String passwordHash,
        List<String> roles) {
    public User {
        roles = roles == null ? List.of("USER") : List.copyOf(roles);
    }
}
