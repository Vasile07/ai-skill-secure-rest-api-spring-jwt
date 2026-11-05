package com.vasile07.readingtracker.user;

import com.vasile07.readingtracker.storage.csv.CsvFileStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CsvUserRepository implements UserRepository {
    private static final String FILE = "users.csv";
    private static final List<String> HEADERS = List.of("id", "name", "email", "passwordHash", "roles");
    private final CsvFileStore store;

    public CsvUserRepository(CsvFileStore store) {
        this.store = store;
    }

    public List<User> findAll() {
        return store.read(FILE, HEADERS).stream().map(this::toUser).toList();
    }

    public Optional<User> findById(UUID id) {
        return findAll().stream().filter(user -> user.id().equals(id)).findFirst();
    }

    public Optional<User> findByEmail(String email) {
        return findAll().stream().filter(user -> user.email().equalsIgnoreCase(email)).findFirst();
    }

    public User save(User user) {
        var users = findAll().stream()
                .filter(existing -> !existing.id().equals(user.id()))
                .map(this::toRow)
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        users.add(toRow(user));
        store.write(FILE, HEADERS, users);
        return user;
    }

    public void deleteById(UUID id) {
        store.write(FILE, HEADERS, findAll().stream()
                .filter(user -> !user.id().equals(id))
                .map(this::toRow)
                .toList());
    }

    private User toUser(List<String> row) {
        String passwordHash = row.size() > 3 ? row.get(3) : "";
        List<String> roles = row.size() > 4 && !row.get(4).isBlank()
                ? List.of(row.get(4).split(";"))
                : List.of("USER");
        return new User(UUID.fromString(row.get(0)), row.get(1), row.get(2), passwordHash, roles);
    }

    private List<String> toRow(User user) {
        return List.of(
                user.id().toString(),
                user.name(),
                user.email(),
                user.passwordHash() == null ? "" : user.passwordHash(),
                String.join(";", user.roles()));
    }
}
