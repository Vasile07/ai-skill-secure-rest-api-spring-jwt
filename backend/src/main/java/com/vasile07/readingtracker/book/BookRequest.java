package com.vasile07.readingtracker.book;

import jakarta.validation.constraints.NotBlank;

public record BookRequest(@NotBlank String title, @NotBlank String author, String isbn, Integer publishedYear) { }
