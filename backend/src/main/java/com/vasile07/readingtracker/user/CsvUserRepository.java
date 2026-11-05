package com.vasile07.readingtracker.user;

import com.vasile07.readingtracker.storage.csv.CsvFileStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CsvUserRepository implements UserRepository {
    private static final String FILE = "users.csv";
    private static final List<String> HEADERS = List.of("id", "name", "email");
    private final CsvFileStore store;

    public CsvUserRepository(CsvFileStore store) { this.store = store; }
    public List<User> findAll() { return store.read(FILE, HEADERS).stream().map(this::toUser).toList(); }
    public Optional<User> findById(UUID id) { return findAll().stream().filter(u -> u.id().equals(id)).findFirst(); }
    public Optional<User> findByEmail(String email) { return findAll().stream().filter(u -> u.email().equalsIgnoreCase(email)).findFirst(); }
    public User save(User user) {
        var users = findAll().stream().filter(existing -> !existing.id().equals(user.id())).map(this::toRow).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        users.add(toRow(user));
        store.write(FILE, HEADERS, users);
        return user;
    }
    public void deleteById(UUID id) {
        store.write(FILE, HEADERS, findAll().stream().filter(u -> !u.id().equals(id)).map(this::toRow).toList());
    }
    private User toUser(List<String> row) { return new User(UUID.fromString(row.get(0)), row.get(1), row.get(2)); }
    private List<String> toRow(User user) { return List.of(user.id().toString(), user.name(), user.email()); }
}
