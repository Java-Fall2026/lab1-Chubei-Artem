package ua.store.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.store.exception.InvalidDataException;
import ua.store.model.Customer;

public final class CustomerLoader {

    private static final Logger log = LoggerFactory.getLogger(CustomerLoader.class);

    private CustomerLoader() {
    }

    /**
     * Parses a single CSV line into a Customer record.
     * Throws checked InvalidDataException on bad input.
     */
    public static Customer parseCustomerFromLine(String line, int lineNumber) throws InvalidDataException {
        if (line == null || line.isBlank()) {
            throw new InvalidDataException("Line " + lineNumber + ": line is empty or blank");
        }

        String[] parts = line.split(",", -1);
        if (parts.length != 3) {
            throw new InvalidDataException("Line " + lineNumber + ": expected 3 columns (email,name,phone), got " + parts.length);
        }

        try {
            // Customer compact constructor validates fields and throws IllegalArgumentException
            return new Customer(parts[0], parts[1], parts[2]);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            // Multi-catch preserving original cause
            throw new InvalidDataException("Line " + lineNumber + ": validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Reads all valid customers from a CSV file using NIO Files API.
     * Corrupted lines are skipped, missing file does not terminate the program.
     */
    public static List<Customer> loadCustomers(Path filePath) {
        log.info("Starting to load customers from file: {}", filePath);
        List<Customer> customers = new ArrayList<>();
        int lineNumber = 0;
        boolean readSuccessful = false;

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            reader.readLine(); // Skip header
            lineNumber++;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    Customer customer = parseCustomerFromLine(line, lineNumber);
                    customers.add(customer);
                    log.info("Successfully loaded customer: {}", customer.email());
                } catch (InvalidDataException e) {
                    log.warn("Corrupted row at line {}: {}. Skipping row.", lineNumber, e.getMessage(), e);
                }
            }
            readSuccessful = true;
            log.info("Finished reading. Total valid customers loaded: {}", customers.size());
        } catch (NoSuchFileException e) {
            log.error("CSV file not found: {}. Returning empty list.", filePath, e);
        } catch (IOException e) {
            log.error("I/O error occurred while reading {}: {}", filePath, e.getMessage(), e);
        } finally {
            log.info("Cleanup/closing completed for file routine: {} (readSuccess={})", filePath, readSuccessful);
        }

        return customers;
    }
}