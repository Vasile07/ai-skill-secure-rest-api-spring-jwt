package com.vasile07.readingtracker.storage.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvFileStore {
    private static final CSVFormat READ_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).get();
    private final Path directory;

    public CsvFileStore(@Value("${app.storage.directory:./data}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public synchronized List<List<String>> read(String fileName, List<String> headers) {
        Path path = file(fileName);
        try {
            Files.createDirectories(directory);
            if (Files.notExists(path)) {
                write(fileName, headers, List.of());
                return List.of();
            }
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                 CSVParser parser = READ_FORMAT.parse(reader)) {
                List<List<String>> rows = new ArrayList<>();
                for (CSVRecord record : parser) {
                    List<String> row = new ArrayList<>();
                    for (String header : headers) row.add(record.get(header));
                    rows.add(row);
                }
                return rows;
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to read CSV file " + path, exception);
        }
    }

    public synchronized void write(String fileName, List<String> headers, List<List<String>> rows) {
        Path destination = file(fileName);
        Path temporary = file(fileName + ".tmp");
        try {
            Files.createDirectories(directory);
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers.toArray(String[]::new)).get())) {
                for (List<String> row : rows) printer.printRecord(row);
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveUnsupported) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write CSV file " + destination, exception);
        }
    }

    private Path file(String fileName) {
        Path path = directory.resolve(fileName).normalize();
        if (!path.getParent().equals(directory)) throw new IllegalArgumentException("Invalid CSV file name");
        return path;
    }
}
