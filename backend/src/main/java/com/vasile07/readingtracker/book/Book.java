package com.vasile07.readingtracker.book;

import java.util.UUID;

public record Book(UUID id, String title, String author, String isbn, Integer publishedYear) { }
