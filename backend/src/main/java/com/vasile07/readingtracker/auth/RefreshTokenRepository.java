package com.vasile07.readingtracker.auth;

import com.vasile07.readingtracker.storage.csv.CsvFileStore;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepository {
    private static final String FILE = "refresh-tokens.csv";
    private static final List<String> HEADERS =
            List.of("tokenHash", "userId", "familyId", "expiresAt", "used", "revoked");

    private final CsvFileStore store;

    public RefreshTokenRepository(CsvFileStore store) {
        this.store = store;
    }

    public synchronized List<Token> all() {
        return store.read(FILE, HEADERS).stream()
                .map(row -> new Token(
                        row.get(0),
                        UUID.fromString(row.get(1)),
                        UUID.fromString(row.get(2)),
                        Instant.parse(row.get(3)),
                        Boolean.parseBoolean(row.get(4)),
                        Boolean.parseBoolean(row.get(5))))
                .toList();
    }

    public synchronized void save(Token token) {
        List<List<String>> rows = new ArrayList<>(all().stream()
                .filter(existing -> !existing.hash().equals(token.hash()))
                .map(this::toRow)
                .toList());
        rows.add(toRow(token));
        store.write(FILE, HEADERS, rows);
    }

    public synchronized Optional<Token> find(String hash) {
        return all().stream().filter(token -> token.hash().equals(hash)).findFirst();
    }

    public synchronized void revokeFamily(UUID familyId) {
        all().stream()
                .filter(token -> token.family().equals(familyId))
                .forEach(token -> save(new Token(
                        token.hash(), token.user(), token.family(), token.expires(), token.used(), true)));
    }

    private List<String> toRow(Token token) {
        return List.of(
                token.hash(),
                token.user().toString(),
                token.family().toString(),
                token.expires().toString(),
                Boolean.toString(token.used()),
                Boolean.toString(token.revoked()));
    }

    public record Token(
            String hash,
            UUID user,
            UUID family,
            Instant expires,
            boolean used,
            boolean revoked) { }
}
