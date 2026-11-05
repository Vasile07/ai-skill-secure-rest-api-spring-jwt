package com.vasile07.readingtracker.bookreading;

import java.util.UUID;

public record BookReading(UUID id, UUID userId, UUID bookId, ReadingStatus status, Integer progressPercent, String notes) { }
