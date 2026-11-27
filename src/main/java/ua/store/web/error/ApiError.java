package ua.store.web.error;

import java.util.List;

/**
 * Standard error response payload returned for all 4xx and 5xx HTTP errors.
 */
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> violations
        ) {

    public record FieldViolation(String field, String message) {

    }

    public ApiError {
        violations = (violations != null) ? List.copyOf(violations) : List.of();
    }

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, List.of());
    }
}
