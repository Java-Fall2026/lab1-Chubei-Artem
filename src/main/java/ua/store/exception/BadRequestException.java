package ua.store.exception;

import java.util.List;

import ua.store.web.error.ApiError.FieldViolation;

public class BadRequestException extends ApiException {

    public BadRequestException(String userMessage) {
        super(400, "Bad Request", userMessage);
    }

    public BadRequestException(String userMessage, Throwable cause) {
        super(400, "Bad Request", userMessage, cause);
    }

    public BadRequestException(String userMessage, List<FieldViolation> violations) {
        super(400, "Bad Request", userMessage, violations);
    }
}
