package ua.store.storage;

import java.util.List;

/**
 * Represents a single rejected record or file-level error during data loading,
 * grouping all validation errors for that record together.
 *
 * @param recordIndex 1-based index of the record in the file (0 if the whole
 * file failed to load)
 * @param errors list of all error descriptions for this record
 */
public record RejectedRecord(int recordIndex, List<String> errors) {

    public RejectedRecord {
        errors = (errors != null) ? List.copyOf(errors) : List.of();
    }

    public RejectedRecord(int recordIndex, String field, String message) {
        this(recordIndex, List.of(field + ": " + message));
    }

    @Override
    public String toString() {
        String joined = String.join("; ", errors);
        if (recordIndex == 0) {
            return "[Файл] " + joined;
        }
        return "[Запис #" + recordIndex + "] " + joined;
    }
}
