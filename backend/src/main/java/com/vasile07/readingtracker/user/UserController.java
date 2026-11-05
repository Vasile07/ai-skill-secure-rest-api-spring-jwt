package com.vasile07.readingtracker.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service = service; }
    @GetMapping public List<User> list() { return service.findAll(); }
    @GetMapping("/{id}") public User get(@PathVariable UUID id) { return service.find(id); }
    @PutMapping("/{id}") public User update(@PathVariable UUID id, @Valid @RequestBody UserRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { service.delete(id); }
}
