package ua.store.exception;

import java.util.List;

import ua.store.web.error.ApiError.FieldViolation;

/**
 * Base unchecked exception carrying HTTP status metadata for
 * ErrorHandlingFilter.
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final String error;
    private final List<FieldViolation> violations;

    public ApiException(int status, String error, String userMessage) {
        this(status, error, userMessage, List.of(), null);
    }

    public ApiException(int status, String error, String userMessage, Throwable cause) {
        this(status, error, userMessage, List.of(), cause);
    }

    public ApiException(int status, String error, String userMessage, List<FieldViolation> violations) {
        this(status, error, userMessage, violations, null);
    }

    private ApiException(
            int status,
            String error,
            String userMessage,
            List<FieldViolation> violations,
            Throwable cause) {
        super(userMessage, cause);
        this.status = status;
        this.error = error;
        this.violations = (violations != null) ? List.copyOf(violations) : List.of();
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public List<FieldViolation> getViolations() {
        return violations;
    }
}
