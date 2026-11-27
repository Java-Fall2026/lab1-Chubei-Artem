package ua.store.exception;

public class UnsupportedMediaTypeException extends ApiException {

    public UnsupportedMediaTypeException(String userMessage) {
        super(415, "Unsupported Media Type", userMessage);
    }
}
