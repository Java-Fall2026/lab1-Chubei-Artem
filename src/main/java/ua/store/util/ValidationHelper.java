package ua.store.util;

import java.time.LocalDate;
import java.util.Set;

final class ValidationHelper {

    private ValidationHelper() {
    }

    static void requireNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null, got: null");
        }
    }

    static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank, got: " + (value == null ? "null" : "\"" + value + "\""));
        }
    }

    static void requireStrictlyPositive(double value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be strictly positive (> 0), got: " + value);
        }
    }

    static void requireNonNegative(double value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be >= 0, got: " + value);
        }
    }

    static void requireInRange(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(fieldName + " must be between " + min + " and " + max + ", got: " + value);
        }
    }

    static void requireNotInFuture(LocalDate date, String fieldName) {
        requireNotNull(date, fieldName);
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(fieldName + " must not be in the future, got: " + date);
        }
    }

    static void requireInAllowedSet(String value, Set<String> allowed, String fieldName) {
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException(fieldName + " must be one of " + allowed + ", got: \"" + value + "\"");
        }
    }
}