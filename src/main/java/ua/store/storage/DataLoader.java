package ua.store.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import ua.store.exception.DuplicateIdentityException;
import ua.store.repository.GenericRepository;

/**
 * Loads entities from JSON/YAML files using FileStorage, validates them with
 * Jakarta Validation, adds valid unique entities to a repository, and collects
 * one RejectedRecord per invalid/duplicate record without failing.
 *
 * @param <T> domain entity type
 */
public class DataLoader<T> implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final FileStorage<T> storage;
    private final Validator validator;
    private final ValidatorFactory ownedFactory;

    public DataLoader(FileStorage<T> storage, Validator validator) {
        this.storage = Objects.requireNonNull(storage, "FileStorage must not be null");
        this.validator = Objects.requireNonNull(validator, "Validator must not be null");
        this.ownedFactory = null;
    }

    public DataLoader(Class<T> type) {
        this.storage = new FileStorage<>(type);
        this.ownedFactory = Validation.buildDefaultValidatorFactory();
        this.validator = this.ownedFactory.getValidator();
    }

    public static Validator createDefaultValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator();
        }
    }

    @Override
    public void close() {
        if (ownedFactory != null) {
            ownedFactory.close();
        }
    }

    /**
     * Loads records from the given file into the target repository. Each
     * invalid or duplicate record produces a single RejectedRecord containing
     * all of its errors.
     */
    public LoadResult loadInto(Path file, GenericRepository<T> repository) {
        Objects.requireNonNull(file, "File path must not be null");
        Objects.requireNonNull(repository, "Target repository must not be null");

        List<T> items;
        try {
            items = storage.load(file);
        } catch (IOException | RuntimeException e) {
            String errorMsg = "Failed to read file '" + file + "': " + e.getMessage();
            log.error("[{}] {}", repository.getName(), errorMsg);
            RejectedRecord fileError = new RejectedRecord(0, "file", errorMsg);
            log.warn("[{}] Rejected record: {}", repository.getName(), fileError);
            return new LoadResult(0, 0, List.of(fileError));
        }

        int totalRead = items.size();
        int addedCount = 0;
        List<RejectedRecord> rejected = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            int recordNumber = i + 1;
            T item = items.get(i);

            if (item == null) {
                RejectedRecord nullRejection = new RejectedRecord(recordNumber, "object", "Запис у файлі є null");
                rejected.add(nullRejection);
                log.warn("[{}] Rejected record: {}", repository.getName(), nullRejection);
                continue;
            }

            Set<ConstraintViolation<T>> violations = validator.validate(item);
            if (!violations.isEmpty()) {
                List<String> recordErrors = violations.stream()
                        .sorted(Comparator.comparing((ConstraintViolation<T> v) -> v.getPropertyPath().toString())
                                .thenComparing(ConstraintViolation::getMessage))
                        .map(v -> {
                            String path = v.getPropertyPath().toString();
                            String fieldName = path.isBlank() ? "object" : path;
                            return fieldName + ": " + v.getMessage();
                        })
                        .toList();

                RejectedRecord rejection = new RejectedRecord(recordNumber, recordErrors);
                rejected.add(rejection);
                log.warn("[{}] Rejected record: {}", repository.getName(), rejection);
                continue;
            }

            try {
                repository.add(item);
                addedCount++;
            } catch (DuplicateIdentityException e) {
                RejectedRecord dupRejection = new RejectedRecord(recordNumber, "duplicate", e.getMessage());
                rejected.add(dupRejection);
                log.warn("[{}] Rejected record: {}", repository.getName(), dupRejection);
            } catch (IllegalArgumentException e) {
                RejectedRecord argRejection = new RejectedRecord(recordNumber, "repository", e.getMessage());
                rejected.add(argRejection);
                log.warn("[{}] Rejected record: {}", repository.getName(), argRejection);
            }
        }

        log.info("[{}] File '{}' processed: read={}, added={}, rejected={}",
                repository.getName(), file, totalRead, addedCount, rejected.size());

        return new LoadResult(totalRead, addedCount, rejected);
    }
}
