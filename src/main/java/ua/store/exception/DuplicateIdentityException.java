package ua.store.exception;

/**
 * Unchecked exception thrown when trying to add an entity with duplicate
 * identity into a repository.
 */
public class DuplicateIdentityException extends IllegalStateException {

    public DuplicateIdentityException(String message) {
        super(message);
    }
}
