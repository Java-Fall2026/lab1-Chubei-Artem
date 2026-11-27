package ua.store.exception;

public class NotFoundException extends ApiException {

    public NotFoundException(String userMessage) {
        super(404, "Not Found", userMessage);
    }
}
