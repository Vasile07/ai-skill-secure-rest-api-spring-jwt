package com.vasile07.readingtracker.bookreading;

import com.vasile07.readingtracker.book.BookRepository;
import com.vasile07.readingtracker.common.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BookReadingService {
    private final BookReadingRepository readings;
    private final BookRepository books;
    public BookReadingService(BookReadingRepository readings, BookRepository books) {
        this.readings = readings;
        this.books = books;
    }

    public List<BookReading> findAll(UUID owner) {
        return readings.findAll().stream().filter(reading -> reading.userId().equals(owner)).toList();
    }

    public BookReading findOwned(UUID id, UUID owner) {
        BookReading reading = readings.findById(id)
                .orElseThrow(() -> new NotFoundException("Book reading not found"));
        if (!reading.userId().equals(owner)) {
            throw new NotFoundException("Book reading not found");
        }
        return reading;
    }

    public BookReading create(BookReadingRequest request, UUID owner) {
        validateReferences(request);
        BookReading reading = new BookReading(
                UUID.randomUUID(), owner, request.bookId(), request.status(),
                request.progressPercent(), request.notes());
        return readings.save(reading);
    }

    public BookReading update(UUID id, BookReadingRequest request, UUID owner) {
        findOwned(id, owner);
        validateReferences(request);
        BookReading reading = new BookReading(
                id, owner, request.bookId(), request.status(), request.progressPercent(), request.notes());
        return readings.save(reading);
    }

    public void delete(UUID id, UUID owner) {
        findOwned(id, owner);
        readings.deleteById(id);
    }

    private void validateReferences(BookReadingRequest request) {
        if (books.findById(request.bookId()).isEmpty()) throw new NotFoundException("Book not found");
    }
}
