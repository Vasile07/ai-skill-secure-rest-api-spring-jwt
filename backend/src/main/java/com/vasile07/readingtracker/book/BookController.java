package com.vasile07.readingtracker.book;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService service;
    public BookController(BookService service) { this.service = service; }
    @GetMapping public List<Book> list() { return service.findAll(); }
    @GetMapping("/{id}") public Book get(@PathVariable UUID id) { return service.find(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Book create(@Valid @RequestBody BookRequest request) { return service.create(request); }
    @PutMapping("/{id}") public Book update(@PathVariable UUID id, @Valid @RequestBody BookRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { service.delete(id); }
}
