package com.vasile07.readingtracker.bookreading;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.security.core.Authentication;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/book-readings")
@SecurityRequirement(name = "bearerAuth")
public class BookReadingController {
    private final BookReadingService service;
    public BookReadingController(BookReadingService service) { this.service = service; }
    @GetMapping
    public List<BookReading> list(Authentication authentication) {
        return service.findAll(caller(authentication));
    }

    @GetMapping("/{id}")
    public BookReading get(@PathVariable UUID id, Authentication authentication) {
        return service.findOwned(id, caller(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookReading create(
            @Valid @RequestBody BookReadingRequest request,
            Authentication authentication) {
        return service.create(request, caller(authentication));
    }

    @PutMapping("/{id}")
    public BookReading update(
            @PathVariable UUID id,
            @Valid @RequestBody BookReadingRequest request,
            Authentication authentication) {
        return service.update(id, request, caller(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        service.delete(id, caller(authentication));
    }

    private UUID caller(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
