package com.vasile07.readingtracker.book;

import com.vasile07.readingtracker.storage.csv.CsvFileStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CsvBookRepository implements BookRepository {
    private static final String FILE = "books.csv";
    private static final List<String> HEADERS = List.of("id", "title", "author", "isbn", "publishedYear");
    private final CsvFileStore store;
    public CsvBookRepository(CsvFileStore store) { this.store = store; }
    public List<Book> findAll() { return store.read(FILE, HEADERS).stream().map(this::toBook).toList(); }
    public Optional<Book> findById(UUID id) { return findAll().stream().filter(book -> book.id().equals(id)).findFirst(); }
    public Book save(Book book) {
        var books = findAll().stream().filter(existing -> !existing.id().equals(book.id())).map(this::toRow).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        books.add(toRow(book));
        store.write(FILE, HEADERS, books);
        return book;
    }
    public void deleteById(UUID id) { store.write(FILE, HEADERS, findAll().stream().filter(book -> !book.id().equals(id)).map(this::toRow).toList()); }
    private Book toBook(List<String> row) { return new Book(UUID.fromString(row.get(0)), row.get(1), row.get(2), row.get(3).isBlank() ? null : row.get(3), row.get(4).isBlank() ? null : Integer.valueOf(row.get(4))); }
    private List<String> toRow(Book book) { return List.of(book.id().toString(), book.title(), book.author(), value(book.isbn()), book.publishedYear() == null ? "" : book.publishedYear().toString()); }
    private String value(String text) { return text == null ? "" : text; }
}
