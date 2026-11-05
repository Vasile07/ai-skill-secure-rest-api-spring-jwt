package com.vasile07.readingtracker.bookreading;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BookReadingRequest(@NotNull UUID userId, @NotNull UUID bookId, @NotNull ReadingStatus status,
                                 @Min(0) @Max(100) Integer progressPercent, String notes) { }
