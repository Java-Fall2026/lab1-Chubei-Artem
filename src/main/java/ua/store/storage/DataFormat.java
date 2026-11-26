package ua.store.storage;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Supported serialization formats determined by file extension.
 */
public enum DataFormat {
    JSON,
    YAML;

    /**
     * Resolves the data format from the given file path extension (.json,
     * .yaml, .yml).
     */
    public static DataFormat fromPath(Path file) {
        Objects.requireNonNull(file, "File path must not be null");
        Path fileNamePath = file.getFileName();
        if (fileNamePath == null) {
            throw new IllegalArgumentException("File path does not contain a file name: " + file);
        }

        String fileName = fileNamePath.toString().toLowerCase(Locale.ROOT);
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("File has no extension: " + fileName);
        }

        String extension = fileName.substring(dotIndex + 1);
        return switch (extension) {
            case "json" ->
                JSON;
            case "yaml", "yml" ->
                YAML;
            default ->
                throw new IllegalArgumentException(
                        "Unsupported file extension '." + extension + "' for file: " + fileName
                );
        };
    }
}
