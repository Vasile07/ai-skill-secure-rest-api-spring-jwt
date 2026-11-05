package com.vasile07.readingtracker.bookreading;

import com.vasile07.readingtracker.book.BookRepository;
import com.vasile07.readingtracker.common.NotFoundException;
import com.vasile07.readingtracker.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BookReadingService {
    private final BookReadingRepository readings;
    private final UserRepository users;
    private final BookRepository books;
    public BookReadingService(BookReadingRepository readings, UserRepository users, BookRepository books) {
        this.readings = readings; this.users = users; this.books = books;
    }
    public List<BookReading> findAll() { return readings.findAll(); }
    public BookReading find(UUID id) { return readings.findById(id).orElseThrow(() -> new NotFoundException("Book reading not found")); }
    public BookReading create(BookReadingRequest request) { validateReferences(request); return readings.save(toReading(UUID.randomUUID(), request)); }
    public BookReading update(UUID id, BookReadingRequest request) { find(id); validateReferences(request); return readings.save(toReading(id, request)); }
    public void delete(UUID id) { find(id); readings.deleteById(id); }
    private void validateReferences(BookReadingRequest request) {
        if (users.findById(request.userId()).isEmpty()) throw new NotFoundException("User not found");
        if (books.findById(request.bookId()).isEmpty()) throw new NotFoundException("Book not found");
    }
    private BookReading toReading(UUID id, BookReadingRequest request) {
        return new BookReading(id, request.userId(), request.bookId(), request.status(), request.progressPercent(), request.notes());
    }
}
