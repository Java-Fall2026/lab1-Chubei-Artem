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
 * detailed rejection reasons without failing.
 *
 * @param <T> domain entity type
 */
public class DataLoader<T> {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final FileStorage<T> storage;
    private final Validator validator;

    public DataLoader(FileStorage<T> storage, Validator validator) {
        this.storage = Objects.requireNonNull(storage, "FileStorage must not be null");
        this.validator = Objects.requireNonNull(validator, "Validator must not be null");
    }

    public DataLoader(Class<T> type) {
        this(new FileStorage<>(type), createDefaultValidator());
    }

    public static Validator createDefaultValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return factory.getValidator();
    }

    /**
     * Loads records from the given file into the target repository. Invalid
     * records, duplicates, and unreadable files are captured in LoadResult
     * without throwing.
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
                List<ConstraintViolation<T>> sortedViolations = violations.stream()
                        .sorted(Comparator.comparing((ConstraintViolation<T> v) -> v.getPropertyPath().toString())
                                .thenComparing(ConstraintViolation::getMessage))
                        .toList();

                for (ConstraintViolation<T> violation : sortedViolations) {
                    String path = violation.getPropertyPath().toString();
                    String fieldName = path.isBlank() ? "object" : path;
                    RejectedRecord rejection = new RejectedRecord(recordNumber, fieldName, violation.getMessage());
                    rejected.add(rejection);
                    log.warn("[{}] Rejected record: {}", repository.getName(), rejection);
                }
                continue;
            }

            try {
                repository.add(item);
                addedCount++;
            } catch (DuplicateIdentityException | IllegalArgumentException e) {
                RejectedRecord dupRejection = new RejectedRecord(recordNumber, "duplicate", e.getMessage());
                rejected.add(dupRejection);
                log.warn("[{}] Rejected record: {}", repository.getName(), dupRejection);
            }
        }

        log.info("[{}] File '{}' processed: read={}, added={}, rejected={}",
                repository.getName(), file, totalRead, addedCount, rejected.size());

        return new LoadResult(totalRead, addedCount, rejected);
    }
}
