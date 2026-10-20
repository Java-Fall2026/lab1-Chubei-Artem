package ua.store.exception;

// Checked exception thrown when encountering corrupted or invalid domain data in external sources.
public class InvalidDataException extends Exception {

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}