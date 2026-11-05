package com.vasile07.readingtracker.user;

import java.util.UUID;

public record User(UUID id, String name, String email) { }
