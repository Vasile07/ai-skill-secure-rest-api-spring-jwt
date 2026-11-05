package com.vasile07.readingtracker.bookreading;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookReadingRepository {
    List<BookReading> findAll();
    Optional<BookReading> findById(UUID id);
    boolean existsByUserId(UUID userId);
    boolean existsByBookId(UUID bookId);
    BookReading save(BookReading reading);
    void deleteById(UUID id);
}
