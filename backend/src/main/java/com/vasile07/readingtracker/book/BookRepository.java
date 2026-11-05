package com.vasile07.readingtracker.book;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository {
    List<Book> findAll();
    Optional<Book> findById(UUID id);
    Book save(Book book);
    void deleteById(UUID id);
}
