package com.vasile07.readingtracker.user;

import com.vasile07.readingtracker.bookreading.BookReadingRepository;
import com.vasile07.readingtracker.common.ConflictException;
import com.vasile07.readingtracker.common.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository users;
    private final BookReadingRepository readings;
    public UserService(UserRepository users, BookReadingRepository readings) { this.users = users; this.readings = readings; }
    public List<User> findAll() { return users.findAll(); }
    public User find(UUID id) { return users.findById(id).orElseThrow(() -> new NotFoundException("User not found")); }
    public User update(UUID id, UserRequest request) {
        find(id);
        users.findByEmail(request.email()).filter(existing -> !existing.id().equals(id)).ifPresent(existing -> { throw new ConflictException("Email is already registered"); });
        User current = find(id);
        return users.save(new User(id, request.name().trim(), request.email().trim().toLowerCase(), current.passwordHash(), current.roles()));
    }
    public void delete(UUID id) {
        find(id);
        if (readings.existsByUserId(id)) throw new ConflictException("Cannot delete a user with book readings");
        users.deleteById(id);
    }
}
