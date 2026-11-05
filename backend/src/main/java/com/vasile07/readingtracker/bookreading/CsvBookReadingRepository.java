package com.vasile07.readingtracker.bookreading;

import com.vasile07.readingtracker.storage.csv.CsvFileStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CsvBookReadingRepository implements BookReadingRepository {
    private static final String FILE = "book-readings.csv";
    private static final List<String> HEADERS = List.of("id", "userId", "bookId", "status", "progressPercent", "notes");
    private final CsvFileStore store;
    public CsvBookReadingRepository(CsvFileStore store) { this.store = store; }
    public List<BookReading> findAll() { return store.read(FILE, HEADERS).stream().map(this::toReading).toList(); }
    public Optional<BookReading> findById(UUID id) { return findAll().stream().filter(item -> item.id().equals(id)).findFirst(); }
    public boolean existsByUserId(UUID userId) { return findAll().stream().anyMatch(item -> item.userId().equals(userId)); }
    public boolean existsByBookId(UUID bookId) { return findAll().stream().anyMatch(item -> item.bookId().equals(bookId)); }
    public BookReading save(BookReading reading) {
        var rows = findAll().stream().filter(existing -> !existing.id().equals(reading.id())).map(this::toRow).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        rows.add(toRow(reading));
        store.write(FILE, HEADERS, rows);
        return reading;
    }
    public void deleteById(UUID id) { store.write(FILE, HEADERS, findAll().stream().filter(item -> !item.id().equals(id)).map(this::toRow).toList()); }
    private BookReading toReading(List<String> row) { return new BookReading(UUID.fromString(row.get(0)), UUID.fromString(row.get(1)), UUID.fromString(row.get(2)), ReadingStatus.valueOf(row.get(3)), row.get(4).isBlank() ? null : Integer.valueOf(row.get(4)), row.get(5).isBlank() ? null : row.get(5)); }
    private List<String> toRow(BookReading item) { return List.of(item.id().toString(), item.userId().toString(), item.bookId().toString(), item.status().name(), item.progressPercent() == null ? "" : item.progressPercent().toString(), item.notes() == null ? "" : item.notes()); }
}
