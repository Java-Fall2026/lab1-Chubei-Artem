package ua.store.exception;

public class MethodNotAllowedException extends ApiException {

    public MethodNotAllowedException(String userMessage) {
        super(405, "Method Not Allowed", userMessage);
    }
}
