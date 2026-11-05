package com.vasile07.readingtracker.common;

import com.vasile07.readingtracker.book.BookRepository;
import com.vasile07.readingtracker.bookreading.BookReadingRepository;
import com.vasile07.readingtracker.user.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    private final UserRepository users;
    private final BookRepository books;
    private final BookReadingRepository readings;

    public AdminController(
            UserRepository users,
            BookRepository books,
            BookReadingRepository readings) {
        this.users = users;
        this.books = books;
        this.readings = readings;
    }

    @GetMapping("/stats")
    public Map<String, Integer> stats() {
        return Map.of(
                "users", users.findAll().size(),
                "books", books.findAll().size(),
                "bookReadings", readings.findAll().size());
    }
}
