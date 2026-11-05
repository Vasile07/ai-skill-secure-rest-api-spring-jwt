package com.vasile07.readingtracker.bookreading;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/book-readings")
public class BookReadingController {
    private final BookReadingService service;
    public BookReadingController(BookReadingService service) { this.service = service; }
    @GetMapping public List<BookReading> list() { return service.findAll(); }
    @GetMapping("/{id}") public BookReading get(@PathVariable UUID id) { return service.find(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public BookReading create(@Valid @RequestBody BookReadingRequest request) { return service.create(request); }
    @PutMapping("/{id}") public BookReading update(@PathVariable UUID id, @Valid @RequestBody BookReadingRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { service.delete(id); }
}
