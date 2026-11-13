package ua.store.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.store.model.Product;

/**
 * Domain repository for Product entities providing custom sorting, Stream API
 * search, grouping, and aggregation operations.
 */
public class ProductRepository extends GenericRepository<Product> {

    private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);

    public ProductRepository() {
        super("products", Product::getSku);
    }

    // === Domain sorting methods ===
    /**
     * Sorts products by title alphabetically (case-insensitive), then by SKU.
     */
    public List<Product> sortByTitleAsc() {
        List<Product> result = findAllSorted(
                Comparator.comparing(Product::getTitle, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Product::getSku)
        );
        log.info("[products] Sorted by title ASC, count={}", result.size());
        return result;
    }

    /**
     * Multi-level sort: by price descending (reversed), then by title
     * ascending.
     */
    public List<Product> sortByPriceDescThenTitleAsc() {
        Comparator<Product> comparator = Comparator.comparingDouble(Product::getPrice)
                .reversed()
                .thenComparing(Product::getTitle, String.CASE_INSENSITIVE_ORDER);
        List<Product> result = findAllSorted(comparator);
        log.info("[products] Multi-level sort (price DESC, title ASC), count={}", result.size());
        return result;
    }

    // === Stream API search methods ===
    /**
     * 1. Search by partial title match (case-insensitive).
     */
    public List<Product> findByTitleContaining(String keyword) {
        Objects.requireNonNull(keyword, "Search keyword must not be null");
        String normalized = keyword.trim().toLowerCase();

        List<Product> result = stream()
                .filter(p -> p.getTitle().toLowerCase().contains(normalized))
                .toList();

        logSearchResult("title containing '" + keyword + "'", result.size());
        return result;
    }

    /**
     * 2. Search by price range [minPrice, maxPrice] inclusive.
     */
    public List<Product> findByPriceRange(double minPrice, double maxPrice) {
        if (minPrice > maxPrice) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }
        List<Product> result = stream()
                .filter(p -> p.getPrice() >= minPrice && p.getPrice() <= maxPrice)
                .sorted()
                .toList();

        logSearchResult("price range [" + minPrice + " .. " + maxPrice + "]", result.size());
        return result;
    }

    /**
     * 3. Search by two conditions simultaneously: title prefix and maximum
     * price, limited to top N. Demonstrates filter, sorted, limit.
     */
    public List<Product> findByTitlePrefixAndMaxPrice(String prefix, double maxPrice, int maxResults) {
        Objects.requireNonNull(prefix, "Title prefix must not be null");
        if (maxResults <= 0) {
            throw new IllegalArgumentException("maxResults must be positive");
        }
        String normalizedPrefix = prefix.trim().toLowerCase();

        List<Product> result = stream()
                .filter(p -> p.getTitle().toLowerCase().startsWith(normalizedPrefix) && p.getPrice() <= maxPrice)
                .sorted(Comparator.comparingDouble(Product::getPrice))
                .limit(maxResults)
                .toList();

        logSearchResult("prefix='" + prefix + "', maxPrice=" + maxPrice + ", limit=" + maxResults, result.size());
        return result;
    }

    /**
     * Extracts distinct product titles sorted alphabetically (demonstrates map,
     * distinct, sorted).
     */
    public List<String> findDistinctTitles() {
        List<String> titles = stream()
                .map(Product::getTitle)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        log.info("[products] Extracted distinct product titles, count={}", titles.size());
        return titles;
    }

    /**
     * Single object search returning Optional via findFirst.
     */
    public Optional<Product> findFirstCheaperThan(double priceThreshold) {
        Optional<Product> found = stream()
                .filter(p -> p.getPrice() < priceThreshold)
                .sorted(Comparator.comparingDouble(Product::getPrice))
                .findFirst();

        if (found.isPresent()) {
            log.info("[products] Found first product cheaper than {}: {}", priceThreshold, found.get().getSku());
        } else {
            log.warn("[products] No product found cheaper than {}", priceThreshold);
        }
        return found;
    }

    // === Grouping and Aggregation ===
    /**
     * Groups products by price tier ("BUDGET", "MID_RANGE", "PREMIUM").
     */
    public Map<String, List<Product>> groupByPriceCategory() {
        Map<String, List<Product>> grouped = stream()
                .collect(Collectors.groupingBy(p -> {
                    if (p.getPrice() < 1000.0) {
                        return "BUDGET";
                    }
                    if (p.getPrice() <= 10000.0) {
                        return "MID_RANGE";
                    }
                    return "PREMIUM";
                }));
        log.info("[products] Grouped products into {} price categories", grouped.size());
        return grouped;
    }

    /**
     * Reduces all product prices to a single total catalog value using reduce.
     * Safe on empty repository (returns 0.0).
     */
    public double calculateTotalCatalogValue() {
        double total = stream()
                .map(Product::getPrice)
                .reduce(0.0, Double::sum);
        log.info("[products] Calculated total catalog value: {}", total);
        return total;
    }

    private void logSearchResult(String criteria, int count) {
        if (count > 0) {
            log.info("[products] Search by [{}] returned {} items", criteria, count);
        } else {
            log.warn("[products] Search by [{}] returned 0 items (empty result)", criteria);
        }
    }
}
