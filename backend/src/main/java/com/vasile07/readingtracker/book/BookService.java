package com.vasile07.readingtracker.book;

import com.vasile07.readingtracker.bookreading.BookReadingRepository;
import com.vasile07.readingtracker.common.ConflictException;
import com.vasile07.readingtracker.common.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BookService {
    private final BookRepository books;
    private final BookReadingRepository readings;
    public BookService(BookRepository books, BookReadingRepository readings) { this.books = books; this.readings = readings; }
    public List<Book> findAll() { return books.findAll(); }
    public Book find(UUID id) { return books.findById(id).orElseThrow(() -> new NotFoundException("Book not found")); }
    public Book create(BookRequest request) { return books.save(toBook(UUID.randomUUID(), request)); }
    public Book update(UUID id, BookRequest request) { find(id); return books.save(toBook(id, request)); }
    public void delete(UUID id) {
        find(id);
        if (readings.existsByBookId(id)) throw new ConflictException("Cannot delete a book that has book readings");
        books.deleteById(id);
    }
    private Book toBook(UUID id, BookRequest request) {
        return new Book(id, request.title().trim(), request.author().trim(), request.isbn() == null ? null : request.isbn().trim(), request.publishedYear());
    }
}
