package ua.store.storage;

/**
 * Represents a single rejected record or file-level error during data loading.
 *
 * @param recordIndex 1-based index of the record in the file (0 if the whole
 * file failed to load)
 * @param field field name or error category (e.g., "price", "sku", "duplicate",
 * "file")
 * @param message human-readable explanation of the rejection reason
 */
public record RejectedRecord(int recordIndex, String field, String message) {

    @Override
    public String toString() {
        if (recordIndex == 0) {
            return "[Файл | " + field + "] " + message;
        }
        return "[Запис #" + recordIndex + " | поле: " + field + "] " + message;
    }
}
