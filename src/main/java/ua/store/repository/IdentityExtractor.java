package ua.store.repository;

/**
 * Functional interface to extract a unique business identity from an entity.
 *
 * @param <T> entity type
 */
@FunctionalInterface
public interface IdentityExtractor<T> {

    /**
     * Extracts unique string identity from the given item.
     *
     * @param item entity instance
     * @return unique string identifier
     */
    String identityOf(T item);
}
