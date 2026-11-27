package ua.store.repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.store.exception.DuplicateIdentityException;

/**
 * Thread-safe generic in-memory repository managing domain entities by their
 * unique business identity.
 *
 * @param <T> entity type
 */
public class GenericRepository<T> {

    private static final Logger log = LoggerFactory.getLogger(GenericRepository.class);

    private final String name;
    private final IdentityExtractor<T> extractor;
    private final Map<String, T> storage = new LinkedHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Primary constructor accepting custom repository name.
     */
    public GenericRepository(String name, IdentityExtractor<T> extractor) {
        this.name = Objects.requireNonNull(name, "Repository name must not be null");
        this.extractor = Objects.requireNonNull(extractor, "IdentityExtractor must not be null");
    }

    /**
     * Overloaded constructor accepting entity Class type.
     */
    public GenericRepository(Class<T> type, IdentityExtractor<T> extractor) {
        this(Objects.requireNonNull(type, "Entity class must not be null").getSimpleName() + "Repository", extractor);
    }

    public String getName() {
        return name;
    }

    /**
     * Extracts and validates the unique business identity of the given entity.
     */
    public String identityOf(T item) {
        return validateAndExtractIdentity(item);
    }

    /**
     * Atomically adds an item. Throws DuplicateIdentityException if identity
     * already exists.
     */
    public void add(T item) {
        String identity = validateAndExtractIdentity(item);
        lock.writeLock().lock();
        try {
            if (storage.containsKey(identity)) {
                log.warn("[{}] Rejected duplicate identity '{}'", name, identity);
                throw new DuplicateIdentityException(
                        "Duplicate identity '" + identity + "' in repository '" + name + "'"
                );
            }
            storage.put(identity, item);
            log.info("[{}] Successfully added item with identity '{}'", name, identity);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Atomically adds item if its identity is not present yet. Returns true if
     * added, false otherwise.
     */
    public boolean addIfAbsent(T item) {
        String identity = validateAndExtractIdentity(item);
        lock.writeLock().lock();
        try {
            if (storage.containsKey(identity)) {
                log.warn("[{}] Duplicate identity '{}' ignored by addIfAbsent", name, identity);
                return false;
            }
            storage.put(identity, item);
            log.info("[{}] Added item with identity '{}' via addIfAbsent", name, identity);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Atomically replaces an existing item having the same identity. If no item
     * with that identity exists, nothing is added and false is returned.
     */
    public boolean replace(T item) {
        String identity = validateAndExtractIdentity(item);
        lock.writeLock().lock();
        try {
            if (!storage.containsKey(identity)) {
                log.warn("[{}] Cannot replace non-existent identity '{}'", name, identity);
                return false;
            }
            storage.put(identity, item);
            log.info("[{}] Replaced item with identity '{}'", name, identity);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Adds all items from collection using PECS (Producer Extends) wildcard.
     */
    public void addAll(Collection<? extends T> items) {
        Objects.requireNonNull(items, "Collection of items must not be null");
        for (T item : items) {
            add(item);
        }
    }

    /**
     * Atomically removes item by identity. Returns true if removed, false
     * otherwise.
     */
    public boolean removeByIdentity(String identity) {
        validateIdentityString(identity);
        lock.writeLock().lock();
        try {
            T removed = storage.remove(identity);
            if (removed != null) {
                log.info("[{}] Removed item with identity '{}'", name, identity);
                return true;
            } else {
                log.warn("[{}] Cannot remove non-existent identity '{}'", name, identity);
                return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Finds item by identity wrapped in Optional.
     */
    public Optional<T> findByIdentity(String identity) {
        validateIdentityString(identity);
        lock.readLock().lock();
        try {
            T item = storage.get(identity);
            if (item != null) {
                log.info("[{}] Found item with identity '{}'", name, identity);
                return Optional.of(item);
            } else {
                log.warn("[{}] Item with identity '{}' not found", name, identity);
                return Optional.empty();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns an unmodifiable snapshot list of all elements to protect internal
     * state and prevent ConcurrentModificationException across threads.
     */
    public List<T> findAll() {
        lock.readLock().lock();
        try {
            return List.copyOf(storage.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a sequential Stream over a thread-safe snapshot of elements in
     * this repository.
     */
    public Stream<T> stream() {
        return findAll().stream();
    }

    /**
     * Returns all elements sorted according to the provided Comparator.
     */
    public List<T> findAllSorted(Comparator<T> comparator) {
        Objects.requireNonNull(comparator, "Comparator must not be null");
        return stream()
                .sorted(comparator)
                .toList();
    }

    /**
     * Returns all elements sorted by their extracted identity in ASC or DESC
     * order.
     */
    public List<T> findAllSortedByIdentity(SortOrder order) {
        Objects.requireNonNull(order, "SortOrder must not be null");
        Comparator<String> keyComparator = order == SortOrder.ASC
                ? Comparator.naturalOrder()
                : Comparator.reverseOrder();

        lock.readLock().lock();
        try {
            List<T> result = storage.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(keyComparator))
                    .map(Map.Entry::getValue)
                    .toList();
            log.info("[{}] Sorted elements by identity ({}), count={}", name, order, result.size());
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns current count of stored items.
     */
    public int size() {
        lock.readLock().lock();
        try {
            return storage.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    private String validateAndExtractIdentity(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null in repository '" + name + "'");
        }
        String identity = extractor.identityOf(item);
        validateIdentityString(identity);
        return identity;
    }

    private void validateIdentityString(String identity) {
        if (identity == null || identity.isBlank()) {
            throw new IllegalArgumentException("Entity identity must not be null or blank in repository '" + name + "'");
        }
    }
}
